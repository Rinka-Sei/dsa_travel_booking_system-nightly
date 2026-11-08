# LLM & AI Agent Output Guide (`llm_code_generation_spec.md`)

This document dictates the required output format and behavioral workflow for any LLM or AI Agent generating code, scripts, or documentation based on the `master_compliance_blueprint.md` compliance framework. 

---

## 1. Output Formatting Rules (Strict)

To ensure outputs are immediately usable by the user, all responses must adhere to the following formatting rules:

- **Raw Markdown Only:** All output must be provided in raw Markdown text. Avoid mixing HTML where Markdown suffices.
- **Code Blocks:** Every source code file (`.java`), database script (`.sql`), and build file (`pom.xml`) must be provided in its own dedicated, copy-ready code block.
- **File Paths:** Always explicitly state the target file path immediately before the code block (e.g., `src/service/MovieCatalog.java`).
- **No Truncation:** Do not use `// ... existing code ...` or placeholder comments. Provide the complete, compilable file contents.
- **Directory Tree:** At the start of any multi-file generation, provide a visual directory tree of the files being created or modified using standard ASCII/Unicode tree characters.
- **Enterprise JavaDocs:** Every `.java` class, interface, and method must include professional, enterprise-grade JavaDoc documentation detailing its purpose, parameters, return values, thrown exceptions, and algorithmic time/space complexity where applicable.

---

## 2. Standard LLM Workflow (Chat / IDE)

When a user requests implementation (e.g., "Build the Movie Management module" or "Implement the Customer Hash Table"), the LLM must output the response in the following sequence:

1.  **Directory Tree:** Provide an updated directory tree reflecting the new files.
2.  **Build Configuration:** Provide the complete `pom.xml` adhering strictly to `build_spec.md` (Java 21, MySQL Connector/J, JUnit 5 only).
3.  **Model Layer:** Provide the POJOs (`model/`).
4.  **DSA Layer:** Provide the Data Structures (`ds/`) with Java Reflection compatibility (if requested by the user).
5.  **Service Layer:** Provide the Service classes (`service/`).
6.  **GUI Layer:** Provide the Swing panels/frames (`gui/`).
7.  **Database Layer:** Provide the SQL scripts and `DatabaseConnection.java`.
8.  **Unit Tests:** Provide JUnit 5 test classes under `src/test/java/ds/` to verify all custom DSA operations (insert, search, delete, sorting).
9.  **Documentation:** Provide the updated `README.md` and `CHANGELOG.md` adhering strictly to the structure defined in `docs_template_spec.md`.

---

## 3. AI Agent Workflow (GitHub / Automated Environments)

When an AI Agent (e.g., an autonomous coding assistant or GitHub Copilot Workspace) is tasked with implementing a feature, it must interact directly with the Git repository using the following automated workflow:

### Step 1: Create a New Branch
The Agent must **never** commit directly to `main` or `master`. It must create a new branch named according to the feature being implemented.
- **Format:** `feature/<short-description>` or `fix/<short-description>`
- **Example:** `feature/movie-bst-implementation`, `feature/customer-hash-crud`

### Step 2: Implement, Document, Test, and Configure
The Agent must write the files to the workspace. This includes:
- Build configuration (`pom.xml`) matching `build_spec.md`.
- Source code (`.java`, `.sql`) complete with Enterprise JavaDocs.
- Unit tests (`src/test/java/ds/*Test.java`).
- Updated `README.md` (following the structure in `docs_template_spec.md`).
- Updated `CHANGELOG.md` (following the structure in `docs_template_spec.md`).

### Step 3: Commit Changes
The Agent must stage and commit the files using a standardized commit message that **begins with a Gitmoji**, followed by the Conventional Commit type and scope.
- **Commit Message Format:** `<gitmoji> <type>(<scope>): <short description>`
- **Example:** `:sparkles: feat(ds): implement MovieBST insert and search with unit tests`
- **Example:** `:construction_worker: feat(gui): add MoviePanel with Update Selected functionality and update docs`
- **Example:** `:bug: fix(gui): unify invoice and itinerary dialogs with inline export buttons`

### Step 4: Push to Remote
The Agent must push the new branch to the remote GitHub repository.
- **Command:** `git push -u origin <branch-name>`

### Step 5: Create a Pull Request (PR)
The Agent must create a Pull Request against the `main` branch, detailing the changes.
- **PR Title:** Matches the primary feature/fix (Gitmoji prefix recommended but optional on PR titles).
- **PR Body:** Must include:
    - Summary of changes.
    - List of files created/modified (including build config, tests, and docs).
    - Verification steps (e.g., "Compiled successfully via `mvn compile`", "Ran `mvn test` with 100% passing rate").

### Agent Execution Pseudocode
If the Agent is operating via terminal, it should execute the following sequence:

    # 1. Branching
    git checkout -b feature/feature-name

    # 2. Adding and committing (assuming files are written)
    git add .
    git commit -m "<gitmoji> <type>(<scope>): implement feature details, unit tests, build config, and update docs"

    # 3. Pushing
    git push -u origin feature/feature-name

    # 4. PR creation via GitHub CLI (if available)
    gh pr create --title "feat(scope): implement feature details" --body "Summary of changes, files modified (source, tests, docs, pom.xml), and testing steps (mvn compile, mvn test)."

---

## 4. Compliance Verification Output

When completing a task, the Agent or LLM must append a short **Compliance Checklist** at the end of the response to prove adherence to the `master_compliance_blueprint.md`, `build_spec.md`, and `docs_template_spec.md` rules:

    ### Compliance Verification
    - [x] Zero external libraries (JDBC & JUnit 5 only)
    - [x] pom.xml adheres to build_spec.md (Java 21 compiler)
    - [x] GUI uses setLayout(null) and setBounds
    - [x] 5-Tier folder structure applied
    - [x] No pre-seeded operational data
    - [x] Full CRUD with "Update Selected" auto-fill
    - [x] DSA integrated (and Reflection used, if requested)
    - [x] Enterprise JavaDocs added to all classes and methods
    - [x] JUnit 5 tests created for all DSA classes
    - [x] README.md and CHANGELOG.md updated per docs_template_spec.md