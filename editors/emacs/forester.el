;;; forester.el --- Forester behavior-tree language support -*- lexical-binding: t; -*-

;; Single-file integration for `.tree' files: a minimal major mode plus
;; eglot (built into Emacs 29+) hooked up to the forester-lsp server.
;;
;; Install: copy this file somewhere and load it from your init.el:
;;
;;   (load "/path/to/forester.el")
;;
;; The server launcher `forester-lsp' must be on PATH (build it with
;; `./gradlew installDist'), or customize `forester-lsp-command'.

;;; Code:

(defgroup forester nil
  "Forester behavior-tree language support."
  :group 'languages)

(defcustom forester-lsp-command '("forester-lsp")
  "Command (list of program and args) used to start the Forester LSP server."
  :type '(repeat string)
  :group 'forester)

(defvar forester-mode-syntax-table
  (let ((table (make-syntax-table)))
    ;; // line comments and /* */ block comments
    (modify-syntax-entry ?/ ". 124" table)
    (modify-syntax-entry ?* ". 23b" table)
    (modify-syntax-entry ?\n ">" table)
    (modify-syntax-entry ?\" "\"" table)
    (modify-syntax-entry ?_ "_" table)
    (modify-syntax-entry ?- "_" table)
    table)
  "Syntax table for `forester-mode'.")

;;;###autoload
(define-derived-mode forester-mode prog-mode "Forester"
  "Major mode for Forester behavior tree (.tree) files."
  :syntax-table forester-mode-syntax-table
  (setq-local comment-start "// ")
  (setq-local comment-end ""))

;;;###autoload
(add-to-list 'auto-mode-alist '("\\.tree\\'" . forester-mode))

(with-eval-after-load 'eglot
  (add-to-list 'eglot-server-programs
               `(forester-mode . ,forester-lsp-command)))

(add-hook 'forester-mode-hook #'eglot-ensure)

(provide 'forester)
;;; forester.el ends here
