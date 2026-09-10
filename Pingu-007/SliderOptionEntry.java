import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Composite Options entry made from a slider and an optional accessory button. */
public final class SliderOptionEntry extends MenuEntry {

    private final Supplier<String> labelSupplier;
    private final MenuSlider slider;
    private final IconButton accessory;

    public SliderOptionEntry(String label, MenuSlider slider, IconButton accessory,
            MenuAction accessoryAction, ControlAction sliderAction) {
        this(constantLabel(label), slider, accessory, accessoryAction, sliderAction);
    }

    public SliderOptionEntry(Supplier<String> labelSupplier, MenuSlider slider, IconButton accessory,
            MenuAction accessoryAction, ControlAction sliderAction) {
        super(accessory == null ? List.of(slider) : List.of(slider, accessory),
                accessory, accessoryAction);
        this.labelSupplier = Objects.requireNonNull(labelSupplier, "labelSupplier");
        this.slider = Objects.requireNonNull(slider, "slider");
        this.accessory = accessory;
        if (sliderAction != null) {
            withPointerAction(sliderAction);
            withoutPointerFocusOnHover(slider);
        }
    }

    public String label() {
        return Objects.requireNonNull(labelSupplier.get(), "label");
    }

    public MenuSlider slider() {
        return slider;
    }

    public IconButton accessory() {
        return accessory;
    }

    private static Supplier<String> constantLabel(String label) {
        String value = Objects.requireNonNull(label, "label");
        return () -> value;
    }
}
