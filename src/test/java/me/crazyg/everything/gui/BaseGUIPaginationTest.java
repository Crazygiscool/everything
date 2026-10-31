package me.crazyg.everything.gui;

import me.crazyg.everything.gui.BaseGUI.Pagination;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseGUIPaginationTest {
    private final List<String> items = List.of("a", "b", "c", "d", "e");

    @Test
    void pagesRoundUp() {
        assertEquals(2, new Pagination(items, 3, 1).getTotalPages());
        assertEquals(3, new Pagination(items, 2, 1).getTotalPages());
    }

    @Test
    void firstPageBounds() {
        Pagination p = new Pagination(items, 3, 1);
        assertEquals(List.of("a", "b", "c"), p.getPageItems());
        assertEquals(0, p.getStartIndex());
        assertTrue(p.hasNext());
        assertTrue(!p.hasPrevious());
    }

    @Test
    void clampsCurrentPage() {
        Pagination p = new Pagination(items, 3, 0);
        assertEquals(1, p.getCurrentPage());
    }

    @Test
    void emptyReturnsEmpty() {
        Pagination p = new Pagination(List.of(), 3, 1);
        assertTrue(p.getPageItems().isEmpty());
        assertEquals(0, p.getTotalPages());
    }
}
