package com.github.besok.foresterlsp;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filesystem access for import resolution: tracks workspace roots, resolves
 * import paths to `.tree` files, and caches parsed documents. When no file is
 * found on disk, falls back to `.tree` resources bundled in the server jar.
 */
public final class Workspace {

    public record ImportedDefinition(String importPath, String name, ForesterDocument.Definition definition,
                                     Path source) {
    }

    private final List<Path> roots = new ArrayList<>();
    private final Map<Path, ForesterDocument> cache = new ConcurrentHashMap<>();
    private final Map<String, ForesterDocument> resourceCache = new ConcurrentHashMap<>();

    public void addRoot(String uri) {
        try {
            Path path = Path.of(URI.create(uri));
            if (Files.isDirectory(path) && !roots.contains(path)) {
                roots.add(path);
            }
        } catch (RuntimeException ignored) {
        }
    }

    public Optional<Path> resolveImportPath(String importPath, String importingUri) {
        for (String candidate : candidatePaths(importPath)) {
            for (Path base : candidateDirs(importingUri)) {
                Path resolved = base.resolve(candidate).toAbsolutePath().normalize();
                if (Files.isRegularFile(resolved)) {
                    return Optional.of(resolved);
                }
            }
        }
        return Optional.empty();
    }


    public ForesterDocument parse(Path path) {
        return cache.computeIfAbsent(path.toAbsolutePath().normalize(), p -> {
            try {
                return ForesterDocument.parse(Files.readString(p));
            } catch (IOException e) {
                return ForesterDocument.parse("");
            }
        });
    }

    public void invalidate(String uri) {
        try {
            Path path = Path.of(URI.create(uri)).toAbsolutePath().normalize();
            cache.remove(path);
        } catch (RuntimeException ignored) {
        }
    }

    public List<Path> treeFiles(String importingUri) {
        List<Path> files = new ArrayList<>();
        for (Path dir : candidateDirs(importingUri)) {
            try (var stream = Files.walk(dir)) {
                stream.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".tree"))
                        .map(p -> p.toAbsolutePath().normalize())
                        .forEach(files::add);
            } catch (IOException ignored) {
            }
        }
        return files.stream().distinct().toList();
    }

    public List<ImportedDefinition> importedDefinitions(ForesterDocument document, String uri) {
        var result = new ArrayList<ImportedDefinition>();
        for (ForesterDocument.Import imp : document.getImports()) {
            Optional<Path> source = resolveImportPath(imp.path(), uri);
            Optional<ForesterDocument> doc = source.isPresent()
                    ? Optional.of(parse(source.get()))
                    : resolveResource(imp.path());
            doc.ifPresent(d -> {
                for (ForesterDocument.Definition definition : d.getDefinitions()) {
                    String name = resolveName(imp, definition.name());
                    if (name != null) {
                        result.add(new ImportedDefinition(imp.path(), name, definition, source.orElse(null)));
                    }
                }
            });
        }
        return result;
    }

    private static String resolveName(ForesterDocument.Import imp, String definitionName) {
        for (ForesterDocument.Import.Alias alias : imp.aliases()) {
            if (alias.name().equals(definitionName)) {
                return alias.rename() != null ? alias.rename() : definitionName;
            }
        }
        return definitionName;
    }

    private Optional<ForesterDocument> resolveResource(String importPath) {
        String resource = relativePath(importPath);
        ForesterDocument document = resourceCache.computeIfAbsent(resource, r -> {
            try (var in = Workspace.class.getResourceAsStream("/" + r)) {
                if (in == null) {
                    return null;
                }
                return ForesterDocument.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                return null;
            }
        });
        return Optional.ofNullable(document);
    }

    private static List<String> candidatePaths(String importPath) {
        if (importPath.endsWith(".tree")) {
            return List.of(importPath);
        }
        return List.of(
                importPath.replace("::", "/") + ".tree",
                importPath + ".tree");
    }

    private static String relativePath(String importPath) {
        if (importPath.endsWith(".tree")) {
            return importPath;
        }
        return importPath.replace("::", "/") + ".tree";
    }

    private List<Path> candidateDirs(String importingUri) {
        var dirs = new ArrayList<Path>();
        try {
            Path importingFile = Path.of(URI.create(importingUri));
            Path dir = importingFile.getParent();
            if (dir != null) {
                dirs.add(dir);
            }
        } catch (RuntimeException ignored) {
        }
        dirs.addAll(roots);
        return dirs;
    }
}
