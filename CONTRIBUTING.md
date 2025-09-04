# Contributing to WindowTester

First off — thanks for taking the time to contribute! 🎉  
The following guidelines will help you set up the project locally, follow our coding standards, and
submit high-quality contributions.

---

## 📋 Before You Start

- Read our [Code of Conduct](CODE_OF_CONDUCT.md) — we take respectful collaboration seriously.
- Check existing [Issues](issues) and [Pull Requests](pulls) to avoid duplication.
- For large changes, please open a discussion or issue first.

---

## 🛠 Local Development Setup

1. **Fork & Clone**

   ```bash
   git clone https://github.com/r4fterman/windowtester.git
   cd windowtester
   ```
2. Run the project

   ```bash
   mvn clean verify
   ```
3. Run the tests

   ```bash
   mvn clean test
   ```

## 🧑‍💻 Coding Guidelines

- Follow the code style enforced by our linter (npm run lint).
- Use descriptive commit messages (e.g., fix: correct typo in readme).
- Keep pull requests focused — one feature or bugfix per PR.

### Code Style & Formatting

- We follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).
- Code formatting is fully automated:
  - The project is configured with [spotless-maven-plugin](https://github.com/diffplug/spotless).
  - A local `pre-commit` hook runs Spotless automatically. See `scripts/git-hooks/pre-commit`
- You do **not** need to manually format your code or run additional commands.
- When you run `git commit`, Spotless will reformat staged Java files if needed.
- If formatting changes were applied, they are automatically added to the commit before it is finalized.

### Developer Notes

- Always commit your changes as usual: `git add ... && git commit -m "message"`.
- If your IDE shows formatting differences, don’t worry – the hook will fix them during commit.
- Pull requests will only be accepted if they are properly formatted (this is enforced by CI as well).

---

Thank you for following the style guide and helping us keep the codebase consistent!

## 🔄 Pull Request Process

1. Create a new branch for your work:

   ```bash
   git checkout -b feature/my-new-feature
   ```
2. Commit your changes with a clear message.
3. Push your branch to your fork:

   ```bash
   git push origin feature/my-new-feature
   ```
4. Open a Pull Request against the main branch.

## 🐞 Reporting Bugs

- Use the GitHub Issue Tracker and create a new entry there.
- Include steps to reproduce, expected behavior, and screenshots if possible.

## 💡 Suggesting Features

- Use the “Feature Request” issue template.
- Clearly explain why the feature is needed and how it should work.

---

Thanks again for contributing to WindowTester — you help make it better for everyone! 🚀
