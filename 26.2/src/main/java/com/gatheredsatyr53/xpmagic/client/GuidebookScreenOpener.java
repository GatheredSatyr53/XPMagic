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
 * <p>Colours and bold are applied through explicit {@link net.minecraft.network.chat.Style} (not
 * legacy § codes), because the book renders the un-styled default text in a low-contrast grey and
 * legacy codes behaved inconsistently here: headings are bold dark-purple, body text is plain black.
 *
 * <p>Page 1 is a clickable table of contents; each entry jumps to a chapter via a {@code change_page}
 * click event, so the page numbers here must match the chapter order in {@link #buildPages()}
 * (change_page is 1-based, and the seven chapters are pages 2..8).
 */
public final class GuidebookScreenOpener {
    private GuidebookScreenOpener() {}

    private static final String[] CHAPTERS = {"storage", "powder", "crystals", "pearl", "gear", "tree", "chips"};

    public static void open() {
        Minecraft.getInstance().setScreenAndShow(new BookViewScreen(new BookViewScreen.BookAccess(buildPages())));
    }

    private static List<Component> buildPages() {
        List<Component> pages = new java.util.ArrayList<>();
        pages.add(contentsPage());
        for (String id : CHAPTERS) {
            pages.add(chapterPage(id));
        }
        return pages;
    }

    /** A chapter page: bold dark-purple heading, blank line, plain black body. */
    private static Component chapterPage(String id) {
        return Component.empty()
            .append(Component.translatable("guide.xpmagic.toc." + id).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD))
            .append(Component.literal("\n\n"))
            .append(Component.translatable("guide.xpmagic.body." + id).withStyle(ChatFormatting.BLACK));
    }

    private static Component contentsPage() {
        MutableComponent page = Component.empty();
        page.append(Component.translatable("guide.xpmagic.title").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        page.append(Component.literal("\n\n"));
        page.append(Component.translatable("guide.xpmagic.intro").withStyle(ChatFormatting.BLACK));
        page.append(Component.literal("\n\n"));
        for (int i = 0; i < CHAPTERS.length; i++) {
            page.append(tocLink(CHAPTERS[i], i + 2)); // chapters start on page 2
        }
        return page;
    }

    /** A clickable table-of-contents line that jumps to the given (1-based) book page. */
    private static MutableComponent tocLink(String id, int page) {
        MutableComponent line = Component.empty()
            .append(Component.literal("» "))
            .append(Component.translatable("guide.xpmagic.toc." + id))
            .withStyle(style -> style
                .withColor(ChatFormatting.BLUE)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.ChangePage(page)));
        return line.append(Component.literal("\n").withStyle(style -> style.withUnderlined(false)));
    }
}
