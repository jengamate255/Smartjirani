# SmartJirani Design References

Reference designs pulled from the Stitch project **SmartJirani Community Network**
(`projects/14356522750771544766`) on 2026-09-28. These are the visual acceptance
baseline for the Mtaa Feed, Resident Profile, Ujamaa Hub and Baraza screens.

All four screens share one shell: a fixed 64dp header (emblem + wordmark + location
chip + notifications bell + avatar) and a four-tab bottom navigation bar
(**Mtaa Feed · Ujamaa Hub · Baraza · Profile**).

| File | Screen | Size | Maps to |
| --- | --- | --- | --- |
| `screens/mtaa-feed.png` / `mtaa-feed.html` | Mtaa Feed | 780×3196 | `feature:feed` — `MtaaFeedScreen` |
| `screens/resident-profile.png` / `resident-profile.html` | Resident Profile | 780×3990 | `feature:profile` — `ResidentProfileScreen` |
| `screens/ujamaa-hub.png` / `ujamaa-hub.html` | Ujamaa Hub | 780×3260 | `feature:ujamaa` — `UjamaaHubScreen` |
| `screens/baraza-events.png` / `baraza-events.html` | Baraza | 780×4342 | `feature:baraza` — `BarazaScreen` |
| `screens/emblem.png` | SmartJirani Emblem | 1024×1024 | brand mark |
| `screens/avatar.png` | Resident avatar | 1024×1024 | profile imagery |
| `PRD.md` | Product Requirements Document | — | product scope |

## Tokens

The designs are authored against the same Ujamaa Modern tokens implemented in
`core/designsystem` — colors in `theme/Color.kt`, the Plus Jakarta Sans scale in
`theme/Type.kt`, radii in `theme/Shape.kt`, spacing in `theme/SmartSpacing.kt`.

## Localization

Screen copy in these HTML files is English. The implemented screens use **Sheng**
(Swahili/Sheng code-switching) as decided for this project, following the tone of
`feature/posts/PostTypeLabels.kt`.
