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

• Follow the code style enforced by our linter (npm run lint).
• Use descriptive commit messages (e.g., fix: correct typo in readme).
• Keep pull requests focused — one feature or bugfix per PR.

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

• Use the GitHub Issue Tracker and create a new entry there.
• Include steps to reproduce, expected behavior, and screenshots if possible.

## 💡 Suggesting Features

• Use the “Feature Request” issue template.
• Clearly explain why the feature is needed and how it should work.

---

Thanks again for contributing to WindowTester — you help make it better for everyone! 🚀