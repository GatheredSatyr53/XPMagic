package com.gatheredsatyr53.xpmagic.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-only: builds the guidebook pages and opens them in the vanilla book screen. Only referenced
 * from {@code GuidebookItem#use} on the logical client, so it never loads on a dedicated server.
 *
 * <p>Colours and bold are applied through explicit styles (not legacy § codes, which rendered
 * inconsistently in the book): headings are bold dark-purple, body text is plain black.
 *
 * <p>The book screen clips whatever doesn't fit and can't scroll (it wraps each page at
 * {@code TEXT_WIDTH} and shows at most {@code MAX_LINES}). So the chapter bodies are paginated here
 * with the real font: a body too long for one page spills onto continuation pages instead of being
 * cut off. The clickable table of contents is built last, once each chapter's first page is known.
 */
public final class GuidebookScreenOpener {
    private GuidebookScreenOpener() {}

    /** Matches BookViewScreen: text wraps at 114px and it renders at most 128/9 = 14 lines. */
    private static final int TEXT_WIDTH = 114;
    private static final int MAX_LINES = 128 / 9;
    /** Lines actually used per page — one below the hard limit, as a safety margin against clipping. */
    private static final int SAFE_LINES = MAX_LINES - 1;

    private static final String[] CHAPTERS = {"storage", "powder", "crystals", "pearl", "gear", "tree", "chips"};

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;

        List<Component> chapterPages = new ArrayList<>();
        int[] chapterStartPage = new int[CHAPTERS.length];
        int pageCursor = 2; // page 1 is the contents; chapters follow (1-based, for change_page)
        for (int i = 0; i < CHAPTERS.length; i++) {
            chapterStartPage[i] = pageCursor;
            List<Component> pages = paginateChapter(font, CHAPTERS[i]);
            chapterPages.addAll(pages);
            pageCursor += pages.size();
        }

        List<Component> pages = new ArrayList<>();
        pages.add(contentsPage(chapterStartPage));
        pages.addAll(chapterPages);

        minecraft.setScreenAndShow(new BookViewScreen(new BookViewScreen.BookAccess(pages)));
    }

    /** Split one chapter into page-sized Components: heading + body, spilling onto extra pages. */
    private static List<Component> paginateChapter(Font font, String id) {
        Component heading = Component.translatable("guide.xpmagic.toc." + id).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD);
        String body = Component.translatable("guide.xpmagic.body." + id).getString();

        List<String> lines = wrap(font, body);
        int headingLines = font.splitIgnoringLanguage(heading, TEXT_WIDTH).size();

        List<Component> pages = new ArrayList<>();
        int index = 0;
        // First page carries the heading plus a blank line, so it holds fewer body lines.
        int firstBudget = Math.max(1, SAFE_LINES - headingLines - 1);
        pages.add(Component.empty()
            .append(heading)
            .append(Component.literal("\n\n"))
            .append(bodyBlock(lines, index, firstBudget)));
        index += Math.min(firstBudget, lines.size() - index);

        while (index < lines.size()) {
            pages.add(bodyBlock(lines, index, SAFE_LINES));
            index += Math.min(SAFE_LINES, lines.size() - index);
        }
        return pages;
    }

    /** A black text block of up to {@code count} pre-wrapped body lines starting at {@code from}. */
    private static Component bodyBlock(List<String> lines, int from, int count) {
        int to = Math.min(from + count, lines.size());
        return Component.literal(String.join("\n", lines.subList(from, to))).withStyle(ChatFormatting.BLACK);
    }

    /** Wrap plain text to the book's text width, returning one string per rendered line. */
    private static List<String> wrap(Font font, String text) {
        List<String> out = new ArrayList<>();
        for (FormattedText line : font.splitIgnoringLanguage(FormattedText.of(text), TEXT_WIDTH)) {
            out.add(line.getString());
        }
        return out;
    }

    private static Component contentsPage(int[] chapterStartPage) {
        MutableComponent page = Component.empty();
        page.append(Component.translatable("guide.xpmagic.title").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        page.append(Component.literal("\n\n"));
        page.append(Component.translatable("guide.xpmagic.intro").withStyle(ChatFormatting.BLACK));
        page.append(Component.literal("\n\n"));
        for (int i = 0; i < CHAPTERS.length; i++) {
            page.append(tocLink(CHAPTERS[i], chapterStartPage[i]));
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
