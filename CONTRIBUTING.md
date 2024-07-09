# Contributing Guidelines

Thanks for your interest in opsu!

This document provides basic guidelines for contributing to the repository. If you plan on making major changes, consider [opening an issue][issues] first to discuss your ideas.

[issues]: https://github.com/clonewith/opsu/issues/new

## Reporting problems

We are happy to see the problems you reported! You can do as belows:

1. Make sure the problem is not about the device itself, and deal with it when necessary.
2. Look through the issues list, and see if your issue was reported before by others.
3. Provide relevant details. The issue template can help you.

If you are unsure about this problem, you can post in our [discussion page][discussions] first.

[discussions]: https://github.com/clonewith/discussions

## Translating

You can use Crowdin to translation opsu! into your language!

For existing language entries, please direct to [the project page on Crowdin](https://crowdin.com/project/opsu) to start translating them. And if your language doesn't appear on the list, feel free to create an issue!

## Making a Change

1. [Fork the repository][fork] and set up your build environment, as described in the [README][buildenv].
2. Make your desired code changes in your fork.
3. Test your change. We have automated CIs that can help check your code, but changes in gameplay and interface still need manual checks. Read the [testing tips](#testing-tips) below for some suggestions.
4. Commit your change and create a [pull request][PR]. Follow up with any requested changes as needed.

[fork]: https://help.github.com/articles/fork-a-repo/
[buildenv]: README.md#building
[PR]: https://help.github.com/articles/creating-a-pull-request-from-a-fork/

## Guidelines

- An issue or a pull request should only contain one feature or bug fix (at least, one exact part of the game). Though PRs with multiple features are also acceptable, those with one exact point are *easier* to review and check.
- Keep the original stucture of source code, like block orders and styles.
- In general, follow the same coding style as the file that you're editing. The `.editorconfig` can help set up your IDE.
- Write comments in your code as needed. At minimum, [Javadoc][Javadoc] comments are expected on all classes, methods, and global variables as to make code easier to read and understand.

[Javadoc]: https://en.wikipedia.org/wiki/Javadoc#Technical_architecture

## Coding Style

- Use tabs for indenting, not spaces.
- Indentation and brace placement follow [Java conventions][indent]. Braces are usually not used for single-statement `if`, `while`, and `for`.
- Avoid upgrading / downgrading dependencies manually.

[indent]: https://en.wikipedia.org/wiki/Indent_style#Variant:_Java

## Testing Tips

- **Gameplay changes:** Depending on the change, consider playing through a regular or [2B][2B] beatmap, watching a replay, pausing/resuming the game, enabling/disabling experimental sliders, etc.
- **UI changes:** Be sure to try different client resolutions (such as 800x600 and widescreen) and different skins (if applicable).
- **Graphics/audio changes:** Test on different operating systems if you can, I'm lack of usable devices at this time xD

## Consider joining us?

Maintaining this huge project by myself *alone* is a challenging task. So I'm appreciated if you are willing to help me in the long term! When you have made up your mind, please email me using the address on my profile. Thanks a lot!

[2B]: https://osu.ppy.sh/s/90935
