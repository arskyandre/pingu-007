# Menu contributor guide

Conventional menus are composed from `MenuEntry` objects and a `MenuController`.
Keep the screen responsible for its own layout and drawing; keep actions and
settings behavior outside visual controls.

## Adding a simple menu

1. Create an `AbstractMenuScreen` subclass.
2. Declare the controls and ordered entries in the constructor.
3. Choose the screen's `MenuInputBindings` and a linear or spatial controller.
4. Implement `layoutContent` with `MenuLayouts` and keep screen-specific
   constants next to the screen.
5. Implement `drawScreen` with local visual choices and `MenuPainter` helpers.
6. Register the screen in `GameCore`'s construction, update, and render paths.

```java
private final List<MenuEntry> entries = List.of(
        MenuEntry.button("CONTINUAR", MenuAction.navigateTo(GameState.PLAYING)),
        MenuEntry.button("OPÇÕES", MenuAction.navigateTo(GameState.OPTIONS)),
        MenuEntry.button("SAIR", MenuAction.navigateTo(GameState.MAIN_MENU)));

private final MenuController controller = MenuController.linear(
        entries, MenuInputBindings.gamepadMenu(), true);
```

`MenuController` owns pointer updates, focus synchronization, navigation,
mouse locking, activation, and shared click feedback. A screen should not
repeat loops for those concerns. Use `withBackAction` for Escape/B/Start
routes and configure its sound policy explicitly.

## Composite entries

Use `MenuEntry.composite` or a small reusable entry type when one focus target
contains multiple controls. `SliderOptionEntry` is the Options example: the
slider and accessory icon are one focus entry, while the model owns the value
change. Use `withAdjustment` for left/right actions and `enabledWhen` for
entries that can become unavailable.

## Options and specialized screens

`OptionsMenu` depends on `OptionsModel`, not `GameCore`. Extend
`OptionsSettings` only when the model needs another narrow setting operation;
adapt production services in `GameOptionsSettings`.

`ShopMenu` intentionally keeps purchasing, quantities, availability, feedback,
and callbacks local. Reuse its typed controls and enabled linear navigation,
but do not move shop economy rules into generic menu classes.
