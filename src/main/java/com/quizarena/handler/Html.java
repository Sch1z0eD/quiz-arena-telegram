package com.quizarena.handler;

final class Html {

    private Html() {
    }

    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
