package com.example;

final class MouseEdgeTracker {
    private static boolean previousPressed;

    private MouseEdgeTracker() {}

    static boolean consumeEdge(boolean pressed) {
        boolean fired = pressed && !previousPressed;
        previousPressed = pressed;
        return fired;
    }
}