# License recommendation (decision required — no license has been applied)

**No `LICENSE` file has been created.** Choosing a license is the copyright holder's decision. Until one is added, the code is "all rights reserved": others may view it on GitHub but have no permission to copy, modify or redistribute it.

## Options commonly used for apps like this

| License | In short | Consider if… |
|---|---|---|
| **MIT** | Very permissive; anyone may use, modify, and sell, including in closed-source apps, if the notice is kept. | You want maximum adoption and don't mind commercial closed-source forks. |
| **Apache-2.0** | Permissive, plus an explicit patent grant and notice requirements. Common in the Android ecosystem. | You want permissive terms with patent protection. |
| **GPL-3.0** | Copyleft; derivative works must also be GPL and open source. Compatible with F-Droid. | You want improvements to stay open. |
| **AGPL-3.0** | GPL plus network-use clause; mostly relevant to server software. | Rarely needed for an offline app. |
| **All rights reserved (no license)** | Source visible, no reuse rights. | You want to publish for viewing only. |

This is general information, not legal advice. A lawyer can advise on your specific situation.

## What the maintainer must supply

1. The chosen license.
2. The copyright holder's name for the notice (for example "Copyright (c) 2026 <name>") and the year(s).

## After you decide

1. Add `LICENSE` with the official text of your choice (copy it from [choosealicense.com](https://choosealicense.com/) or the GitHub "Add file → license" picker, which fills the template).
2. Update the **License** section in `README.md`.
3. Update the last line of `CONTRIBUTING.md` to state contribution terms.
4. Optionally add `SPDX-License-Identifier` headers.
5. Check third-party dependency licenses (AndroidX, Compose, Room and Kotlin are Apache-2.0 licensed; verify against the versions you ship).
