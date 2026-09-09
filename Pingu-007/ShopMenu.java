import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/** Specialized shop screen that reuses menu controls without generalizing purchase rules. */
public final class ShopMenu implements MenuScreen {

    private final List<ShopItem> items = new ArrayList<>();
    private final List<ShopItemButton> buttons = new ArrayList<>();
    private final List<MenuEntry> itemEntries = new ArrayList<>();
    private final SoundManager soundManager;
    private final MenuInputBindings inputBindings = MenuInputBindings.shopMenu();
    private final MenuController itemController;
    private final IconButton quantityLeftButton;
    private final IconButton quantityRightButton;
    private final BufferedImage coinIcon;

    private Runnable onClose;
    private Player player;
    private int selectedIndex;
    private int selectedQuantity = 1;
    private boolean open;
    private String feedbackMessage;
    private int feedbackTimer;
    private MenuViewport lastLayoutViewport;
    private int lastLayoutSelection = -1;

    private static final int FEEDBACK_DURATION = 90;

    private static final int BUTTON_WIDTH = 360;
    private static final int BUTTON_HEIGHT = 64;
    private static final int BUTTON_GAP = 10;
    private static final int LIST_MARGIN_LEFT = 70;
    private static final int TOP_MARGIN = 70;
    private static final int HEADER_GAP = 26;
    private static final int PANEL_GAP = 60;
    private static final int DETAIL_ITEM_ICON_SIZE = 56;
    private static final int DETAIL_ITEM_ICON_GAP = 14;
    private static final int QUANTITY_BUTTON_SIZE = 36;
    private static final int QUANTITY_NUMBER_HALF_GAP = 34;
    private static final int QUANTITY_TOP_GAP = 8;
    private static final int QUANTITY_LABEL_GAP = 12;
    private static final int TOTAL_TOP_GAP = 10;

    public ShopMenu(SoundManager soundManager) {
        this.soundManager = soundManager;
        quantityLeftButton = new IconButton(0, 0, QUANTITY_BUTTON_SIZE, IconIndex.LEFT_ARROW, false);
        quantityRightButton = new IconButton(0, 0, QUANTITY_BUTTON_SIZE, IconIndex.RIGHT_ARROW, false);
        BufferedImage loadedCoinIcon = null;
        try {
            loadedCoinIcon = LoadSave.GetSpriteAtlas("images/hud/moedasprite.png").getSubimage(16, 0, 16, 16);
        } catch (Exception exception) {
            System.err.println("ShopMenu: erro ao carregar moedasprite.png: " + exception.getMessage());
        }
        coinIcon = loadedCoinIcon;

        itemController = MenuController.linear(itemEntries, inputBindings, true)
                .withMouseLockOnNavigation(false)
                .withMouseLockArbitration(false)
                .withActivationSound(false)
                .withBackAction(() -> {
                    fechar();
                    return GameState.PLAYING;
                }, true);
    }

    public void addItem(String nome, String descricao, BufferedImage icone, int preco,
            Runnable aoComprar, boolean disponivel, boolean compraUnica) {
        ShopItem item = new ShopItem(nome, descricao, icone, preco, aoComprar, disponivel, compraUnica);
        int itemIndex = items.size();
        ShopItemButton button = new ShopItemButton(item, coinIcon, 0, 0,
                BUTTON_WIDTH, BUTTON_HEIGHT);
        items.add(item);
        buttons.add(button);
        itemEntries.add(MenuEntry.button(button,
                MenuAction.stay(GameState.SHOP, () -> comprarItem(itemIndex)))
                .enabledWhen(() -> item.disponivel));
    }

    public void limparItens() {
        items.clear();
        buttons.clear();
        itemEntries.clear();
        selectedIndex = 0;
        selectedQuantity = 1;
        itemController.navigator().setFocusedIndex(0);
        invalidateLayout();
    }

    public boolean isAberto() {
        return open;
    }

    public void setAoFechar(Runnable callback) {
        onClose = callback;
    }

    public void abrir(Player player) {
        this.player = player;
        selectedIndex = 0;
        selectedQuantity = 1;
        feedbackMessage = null;
        feedbackTimer = 0;
        open = true;
        itemController.navigator().setFocusedIndex(0);
        invalidateLayout();
        GameCore.setGameState(GameState.SHOP);
    }

    public void fechar() {
        open = false;
        GameCore.setGameState(GameState.PLAYING);
        if (onClose != null) {
            Runnable callback = onClose;
            onClose = null;
            callback.run();
        }
    }

    private void invalidateLayout() {
        lastLayoutViewport = null;
        lastLayoutSelection = -1;
    }

    @Override
    public void layout(MenuViewport viewport) {
        if (viewport.equals(lastLayoutViewport) && selectedIndex == lastLayoutSelection) {
            return;
        }
        repositionButtons();
        positionQuantityButtonsForSelection(viewport);
        lastLayoutViewport = viewport;
        lastLayoutSelection = selectedIndex;
    }

    private void repositionButtons() {
        int y = TOP_MARGIN + HEADER_GAP;
        for (ShopItemButton button : buttons) {
            button.setPosition(LIST_MARGIN_LEFT, y);
            y += button.getBoundsCopy().height + BUTTON_GAP;
        }
    }

    private void positionQuantityButtonsForSelection(MenuViewport viewport) {
        if (items.isEmpty() || selectedIndex < 0 || selectedIndex >= items.size()
                || items.get(selectedIndex).compra_unica) {
            return;
        }
        positionQuantityButtonsForItem(viewport.width(), items.get(selectedIndex));
    }

    @Override
    public GameState update(MenuContext context) {
        if (!open) {
            return GameState.SHOP;
        }
        layout(context.viewport());
        garantirSelecaoDisponivel(1);

        if (feedbackTimer > 0) {
            feedbackTimer--;
        }

        GameState controllerResult = itemController.update(context, GameState.SHOP);
        sincronizarSelecaoDoNavegador();
        if (controllerResult != GameState.SHOP) {
            return controllerResult;
        }

        if (items.isEmpty()) {
            return GameState.SHOP;
        }

        garantirSelecaoDisponivel(1);
        ShopItem selectedItem = items.get(selectedIndex);
        ajustarQuantidadeAoLimite(selectedItem);
        positionQuantityButtonsForItem(context.viewport().width(), selectedItem);

        if (!selectedItem.compra_unica) {
            if (inputBindings.isJustPressed(MenuInputIntent.LEFT, context.input())) {
                alterarQuantidade(-1, selectedItem);
                soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            }
            if (inputBindings.isJustPressed(MenuInputIntent.RIGHT, context.input())) {
                alterarQuantidade(1, selectedItem);
                soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            }
            if (quantityLeftButton.updatePointer(context.input()) == MenuInteraction.CLICKED) {
                alterarQuantidade(-1, selectedItem);
                soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            }
            if (quantityRightButton.updatePointer(context.input()) == MenuInteraction.CLICKED) {
                alterarQuantidade(1, selectedItem);
                soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            }
        }
        return GameState.SHOP;
    }

    private void sincronizarSelecaoDoNavegador() {
        int focusedIndex = itemController.navigator().focusedIndex();
        if (focusedIndex >= 0 && focusedIndex < items.size() && focusedIndex != selectedIndex) {
            selectedIndex = focusedIndex;
            selectedQuantity = 1;
            lastLayoutSelection = -1;
        }
        sincronizarSelecaoVisual();
    }

    private void sincronizarSelecaoVisual() {
        for (int index = 0; index < buttons.size(); index++) {
            buttons.get(index).setSelected(index == selectedIndex);
        }
    }

    private void comprarItem(int index) {
        ShopItem item = items.get(index);
        if (!item.disponivel) {
            feedbackMessage = "Item já adquirido!";
            feedbackTimer = FEEDBACK_DURATION;
            soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            return;
        }

        int quantity = item.compra_unica ? 1 : selectedQuantity;
        long total = (long) item.preco * quantity;
        if (player.getMoedas() >= total) {
            player.addMoedas(-(int) total);
            if (item.aoComprar != null) {
                for (int indexInPurchase = 0; indexInPurchase < quantity; indexInPurchase++) {
                    item.aoComprar.run();
                }
            }
            if (item.compra_unica) {
                item.disponivel = false;
            }
            soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            soundManager.playSFX(SoundManager.SFX.NOOT_NOOT);
            feedbackMessage = "Comprou: " + item.nome
                    + (quantity > 1 ? " x" + quantity : "") + "!";
            garantirSelecaoDisponivel(1);
            ajustarQuantidadeAoLimite(item);
        } else {
            soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            feedbackMessage = "Moedas insuficientes!";
        }
        feedbackTimer = FEEDBACK_DURATION;
    }

    @Override
    public void render(Graphics2D graphics, MenuViewport viewport) {
        if (!open) {
            return;
        }
        layout(viewport);
        sincronizarSelecaoVisual();

        Graphics2D copy = (Graphics2D) graphics.create();
        try {
            copy.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            copy.setColor(new Color(0, 0, 0, 190));
            copy.fillRect(0, 0, viewport.width(), viewport.height());

            drawListHeader(copy);
            drawItemList(copy);
            drawDetailPanel(copy, viewport.width(), viewport.height());
            drawFeedback(copy, viewport.width(), viewport.height());
            drawControlsHint(copy, viewport.width(), viewport.height());
            copy.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
        } finally {
            copy.dispose();
        }
    }

    private void drawTextWithShadow(Graphics2D graphics, String text, int x, int y, Color color) {
        MenuPainter.drawTextWithShadow(graphics, text, x, y, color,
                new Color(0, 0, 0, 160), 2, 2);
    }

    private void drawListHeader(Graphics2D graphics) {
        graphics.setFont(MenuFonts.buttonFont(11f));
        String itemsLabel = "ITENS";
        String priceLabel = "PREÇO";
        FontMetrics metrics = graphics.getFontMetrics();

        int headerY = TOP_MARGIN;
        drawTextWithShadow(graphics, itemsLabel, LIST_MARGIN_LEFT, headerY, new Color(220, 220, 220, 200));

        int priceLabelX = LIST_MARGIN_LEFT + BUTTON_WIDTH - metrics.stringWidth(priceLabel);
        drawTextWithShadow(graphics, priceLabel, priceLabelX, headerY, new Color(220, 220, 220, 200));

        int lineY = headerY + 6;
        int lineStartX = LIST_MARGIN_LEFT + metrics.stringWidth(itemsLabel) + 10;
        int lineEndX = priceLabelX - 10;
        graphics.setColor(new Color(255, 255, 255, 90));
        graphics.drawLine(lineStartX, lineY, lineEndX, lineY);
    }

    private void drawItemList(Graphics2D graphics) {
        for (ShopItemButton button : buttons) {
            button.render(graphics);
        }
    }

    private void drawDetailPanel(Graphics2D graphics, int screenWidth, int screenHeight) {
        int listRightEdge = LIST_MARGIN_LEFT + BUTTON_WIDTH;
        int panelX = listRightEdge + PANEL_GAP;
        int panelWidth = Math.max(200, screenWidth - panelX - 60);

        if (items.isEmpty()) {
            graphics.setFont(MenuFonts.gameTextFont(14f));
            drawTextWithShadow(graphics, "Nenhum item disponível.", panelX,
                    TOP_MARGIN + HEADER_GAP + BUTTON_HEIGHT / 2, Color.LIGHT_GRAY);
            return;
        }

        ShopItem item = items.get(selectedIndex);
        int y = TOP_MARGIN + HEADER_GAP;

        graphics.setFont(MenuFonts.gameTextFont(22f));
        FontMetrics valueMetrics = graphics.getFontMetrics();
        int iconSize = 22;
        if (coinIcon != null) {
            graphics.drawImage(coinIcon, panelX, y - iconSize + 4, iconSize, iconSize, null);
        }
        String priceText = String.valueOf(item.preco);
        int textX = panelX + iconSize + 8;
        drawTextWithShadow(graphics, priceText, textX, y, Color.WHITE);

        graphics.setFont(MenuFonts.gameTextFont(11f));
        String priceLabel = "Preço";
        FontMetrics labelMetrics = graphics.getFontMetrics();
        int labelX = panelX + panelWidth - labelMetrics.stringWidth(priceLabel);
        drawTextWithShadow(graphics, priceLabel, labelX, y, new Color(210, 210, 210, 170));

        int dashStartX = textX + valueMetrics.stringWidth(priceText) + 14;
        int dashEndX = labelX - 14;
        if (dashEndX > dashStartX) {
            drawDashedLine(graphics, dashStartX, y - valueMetrics.getAscent() / 2, dashEndX);
        }

        y += 50;
        graphics.setFont(MenuFonts.gameTextFont(26f));
        FontMetrics nameMetrics = graphics.getFontMetrics();
        int nameX = panelX;
        if (item.icone != null && item.icone != GameCore.missing_image) {
            int iconX = panelX;
            int textCenterY = y - (nameMetrics.getAscent() - nameMetrics.getDescent()) / 2;
            int iconY = textCenterY - DETAIL_ITEM_ICON_SIZE / 2;
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.setColor(new Color(0, 0, 0, 72));
            graphics.fillRoundRect(iconX, iconY, DETAIL_ITEM_ICON_SIZE, DETAIL_ITEM_ICON_SIZE, 6, 6);
            graphics.drawImage(item.icone, iconX, iconY, DETAIL_ITEM_ICON_SIZE, DETAIL_ITEM_ICON_SIZE, null);
            nameX += DETAIL_ITEM_ICON_SIZE + DETAIL_ITEM_ICON_GAP;
        }

        drawTextWithShadow(graphics, item.nome, nameX, y, Color.WHITE);
        y += 40;

        graphics.setFont(MenuFonts.gameTextFont(12f));
        FontMetrics descriptionMetrics = graphics.getFontMetrics();
        List<String> lines = MenuPainter.wrapWordsWithEndl(descriptionMetrics, item.descricao, panelWidth);
        int lineHeight = descriptionMetrics.getHeight() + 6;
        for (String line : lines) {
            drawTextWithShadow(graphics, line, panelX, y, new Color(220, 220, 220));
            y += lineHeight;
        }

        int totalY;
        if (!item.compra_unica) {
            ajustarQuantidadeAoLimite(item);
            int selectorY = y + QUANTITY_TOP_GAP;
            positionQuantityButtons(panelX, panelWidth, selectorY);
            drawQuantitySelector(graphics, item);
            totalY = selectorY + QUANTITY_BUTTON_SIZE + TOTAL_TOP_GAP;
        } else {
            totalY = y + QUANTITY_TOP_GAP;
        }
        drawTotal(graphics, item, panelX, panelWidth, totalY);
    }

    private void positionQuantityButtonsForItem(int screenWidth, ShopItem item) {
        int listRightEdge = LIST_MARGIN_LEFT + BUTTON_WIDTH;
        int panelX = listRightEdge + PANEL_GAP;
        int panelWidth = Math.max(200, screenWidth - panelX - 60);

        BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = dummy.createGraphics();
        try {
            graphics.setFont(MenuFonts.gameTextFont(12f));
            FontMetrics metrics = graphics.getFontMetrics();
            int descriptionLines = MenuPainter.wrapWordsWithEndl(metrics, item.descricao, panelWidth).size();
            int lineHeight = metrics.getHeight() + 6;
            int descriptionY = TOP_MARGIN + HEADER_GAP + 50 + 40;
            int selectorY = descriptionY + descriptionLines * lineHeight + QUANTITY_TOP_GAP;
            positionQuantityButtons(panelX, panelWidth, selectorY);
        } finally {
            graphics.dispose();
        }
    }

    private void positionQuantityButtons(int panelX, int panelWidth, int y) {
        int centerX = panelX + panelWidth / 2;
        quantityLeftButton.setPosition(centerX - QUANTITY_NUMBER_HALF_GAP - QUANTITY_BUTTON_SIZE, y);
        quantityRightButton.setPosition(centerX + QUANTITY_NUMBER_HALF_GAP, y);
    }

    private void drawQuantitySelector(Graphics2D graphics, ShopItem item) {
        quantityLeftButton.render(graphics);
        quantityRightButton.render(graphics);

        String label = "Quantidade:";
        graphics.setFont(MenuFonts.gameTextFont(12f));
        FontMetrics labelMetrics = graphics.getFontMetrics();
        Rectangle leftBounds = quantityLeftButton.getBoundsCopy();
        int labelX = leftBounds.x - QUANTITY_LABEL_GAP - labelMetrics.stringWidth(label);
        int labelY = leftBounds.y + (leftBounds.height - labelMetrics.getHeight()) / 2
                + labelMetrics.getAscent();
        drawTextWithShadow(graphics, label, labelX, labelY, new Color(220, 220, 220));

        String quantityText = String.valueOf(selectedQuantity);
        graphics.setFont(MenuFonts.gameTextFont(15f));
        FontMetrics quantityMetrics = graphics.getFontMetrics();
        Rectangle rightBounds = quantityRightButton.getBoundsCopy();
        int centerX = (leftBounds.x + leftBounds.width + rightBounds.x) / 2;
        int x = centerX - quantityMetrics.stringWidth(quantityText) / 2;
        int y = leftBounds.y + (leftBounds.height - quantityMetrics.getHeight()) / 2
                + quantityMetrics.getAscent();
        drawTextWithShadow(graphics, quantityText, x, y,
                podeComprar(item) ? new Color(255, 215, 80) : new Color(220, 90, 90));
    }

    private void alterarQuantidade(int direction, ShopItem item) {
        int limit = getLimiteQuantidade(item);
        if (limit <= 1) {
            selectedQuantity = 1;
        } else if (direction < 0) {
            selectedQuantity = selectedQuantity == 1 ? limit : selectedQuantity - 1;
        } else {
            selectedQuantity = selectedQuantity == limit ? 1 : selectedQuantity + 1;
        }
    }

    private void ajustarQuantidadeAoLimite(ShopItem item) {
        if (item.compra_unica) {
            selectedQuantity = 1;
            return;
        }
        selectedQuantity = Math.max(1, Math.min(selectedQuantity, getLimiteQuantidade(item)));
    }

    private int getLimiteQuantidade(ShopItem item) {
        if (player == null || item.preco <= 0) {
            return 1;
        }
        long coins = Math.max(0, player.getMoedas());
        long affordableQuantity = coins / item.preco;
        long nextTen = ((affordableQuantity + 9) / 10) * 10;
        nextTen = Math.max(10, nextTen);
        return (int) Math.min(Integer.MAX_VALUE, nextTen);
    }

    private long getTotal(ShopItem item) {
        int quantity = item.compra_unica ? 1 : selectedQuantity;
        return (long) item.preco * quantity;
    }

    private boolean podeComprar(ShopItem item) {
        return player != null && player.getMoedas() >= getTotal(item);
    }

    private void drawTotal(Graphics2D graphics, ShopItem item, int panelX, int panelWidth, int topY) {
        String text = "Total: " + getTotal(item) + " moedas";
        graphics.setFont(MenuFonts.gameTextFont(15f));
        FontMetrics metrics = graphics.getFontMetrics();
        int x = panelX + (panelWidth - metrics.stringWidth(text)) / 2;
        int y = topY + metrics.getAscent();
        drawTextWithShadow(graphics, text, x, y,
                podeComprar(item) ? new Color(255, 215, 80) : new Color(220, 90, 90));
    }

    private void drawFeedback(Graphics2D graphics, int screenWidth, int screenHeight) {
        if (feedbackMessage == null || feedbackTimer <= 0) {
            return;
        }
        graphics.setFont(MenuFonts.gameTextFont(12f));
        FontMetrics metrics = graphics.getFontMetrics();
        int x = (screenWidth - metrics.stringWidth(feedbackMessage)) / 2;
        int y = screenHeight - 90;
        drawTextWithShadow(graphics, feedbackMessage, x, y, new Color(150, 230, 150));
    }

    private void drawControlsHint(Graphics2D graphics, int screenWidth, int screenHeight) {
        graphics.setFont(MenuFonts.gameTextFont(10f));
        String text = "[DPAD] Navegar  [A] Comprar  [ESC/B/START] Sair";
        FontMetrics metrics = graphics.getFontMetrics();
        int x = (screenWidth - metrics.stringWidth(text)) / 2;
        int y = screenHeight - 30;
        drawTextWithShadow(graphics, text, x, y, new Color(200, 200, 200));
    }

    private void drawDashedLine(Graphics2D graphics, int x1, int y, int x2) {
        MenuPainter.drawDashedLine(graphics, x1, y, x2,
                new Color(255, 255, 255, 90), 1f, new float[] { 2, 4 }, 0);
    }

    private void garantirSelecaoDisponivel(int preferredDirection) {
        if (items.isEmpty()) {
            selectedIndex = 0;
            selectedQuantity = 1;
            return;
        }
        itemController.navigator().ensureFocusedEntry(preferredDirection);
        sincronizarSelecaoDoNavegador();
    }
}
