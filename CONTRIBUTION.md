# Contributing to Everything

Thank you for your interest in contributing to **Everything**!  
This project is built with a focus on **modularity**, **maintainability**, and **future‑proof architecture**.  
Before submitting changes, please review the guidelines below to ensure consistency and quality.

---

## 1. Project Philosophy

Everything is designed around:

- **clean separation of concerns** — UI, logic, and data must remain isolated  
- **extensibility** — new modules should plug in without modifying core systems  
- **predictable architecture** — contributors should always know where code belongs  
- **safety and stability** — no silent failures, no unsafe assumptions  

If your contribution aligns with these principles, you’re already on the right track.

---

## 2. How to Contribute

### Fork & Clone

- **Fork the repository**  
- **Clone your fork locally**  
- **Create a new branch** for your feature or fix

### Branch Naming

Use descriptive branch names:

- `feature/help-gui-pagination`
- `fix/warp-command-npe`
- `refactor/gui-framework`

---

## 3. Code Style Guidelines

To maintain consistency:

- **Use clear, explicit naming**  
- **Avoid static utility abuse** — prefer injected managers  
- **Follow the existing package structure**  
- **Document complex logic** with concise comments  
- **Never hardcode messages** — use config or message manager  

### Java Standards(well, follow to your best attempt)

- 4‑space indentation  
- best to not use wildcard imports  
- Prefer `final` where appropriate  
- Avoid deeply nested logic  

---

## 4. GUI Development Rules(still very experimental)

When contributing GUI components:

- **Extend BaseGUI** — never create raw inventories  
- **Use ItemBuilder** for all item creation  
- **Do not bypass GUIListener**  
- **Keep GUIs stateless** — no persistent state unless intentional  
- **Follow the slot layout conventions**  

---

## 5. Submitting a Pull Request

Before opening a PR:

- **Ensure your branch is up to date**  
- **Test your changes in‑game**  
- **Write a clear PR description**  
- **Link related issues** if applicable  

Your PR should include:

- What you changed  
- Why you changed it  
- How it improves the project  
- Any potential side effects  

---

## 6. Reporting Issues

If you find a bug:

- **Describe the issue clearly**  
- **Include reproduction steps**  
- **Attach logs or screenshots**  
- **Specify server version and platform**  

---

## 7. Feature Requests

We welcome new ideas!  
When suggesting a feature:

- **Explain the use case**  
- **Describe expected behavior**  
- **Provide examples or mockups**  
- **Ensure it aligns with project philosophy**  

---

## 8. Licensing

By contributing, you agree that your code will be released under the project’s license.  
This ensures **open collaboration** and **long‑term maintainability**.

---

## 9. Thank You

Your contributions help Everything grow into a powerful, modular, and community‑driven project.  
We appreciate every improvement — big or small.(seriousely, even if you just change a chat sentence)
