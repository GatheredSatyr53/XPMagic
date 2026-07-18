package com.gatheredsatyr53.xpmagic.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

/**
 * Client-only: builds the guidebook pages and opens them in the vanilla book screen. Only referenced
 * from {@code GuidebookItem#use} on the logical client, so it never loads on a dedicated server.
 *
 * <p>Page 1 is a clickable table of contents; each entry jumps to a chapter via a {@code change_page}
 * click event (the same mechanism vanilla written books use), so page numbers here must match the
 * chapter order in {@link #buildPages()} (change_page is 1-based).
 */
public final class GuidebookScreenOpener {
    private GuidebookScreenOpener() {}

    public static void open() {
        Minecraft.getInstance().setScreenAndShow(new BookViewScreen(new BookViewScreen.BookAccess(buildPages())));
    }

    private static List<Component> buildPages() {
        return List.of(
            contentsPage(),
            Component.translatable("guide.xpmagic.page.storage"),
            Component.translatable("guide.xpmagic.page.powder"),
            Component.translatable("guide.xpmagic.page.crystals"),
            Component.translatable("guide.xpmagic.page.pearl"),
            Component.translatable("guide.xpmagic.page.gear"),
            Component.translatable("guide.xpmagic.page.tree"),
            Component.translatable("guide.xpmagic.page.chips")
        );
    }

    private static Component contentsPage() {
        MutableComponent page = Component.empty();
        page.append(Component.translatable("guide.xpmagic.title").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        page.append(Component.literal("\n\n"));
        page.append(Component.translatable("guide.xpmagic.intro").withStyle(ChatFormatting.DARK_GRAY));
        page.append(Component.literal("\n\n"));
        page.append(link("guide.xpmagic.toc.storage", 2));
        page.append(link("guide.xpmagic.toc.powder", 3));
        page.append(link("guide.xpmagic.toc.crystals", 4));
        page.append(link("guide.xpmagic.toc.pearl", 5));
        page.append(link("guide.xpmagic.toc.gear", 6));
        page.append(link("guide.xpmagic.toc.tree", 7));
        page.append(link("guide.xpmagic.toc.chips", 8));
        return page;
    }

    /** A clickable table-of-contents line that jumps to the given (1-based) book page. */
    private static MutableComponent link(String key, int page) {
        MutableComponent line = Component.translatable(key).withStyle(style -> style
            .withColor(ChatFormatting.BLUE)
            .withUnderlined(true)
            .withClickEvent(new ClickEvent.ChangePage(page)));
        return line.append(Component.literal("\n").withStyle(style -> style.withUnderlined(false)));
    }
}
