package sk.uniza.fri.cp.App;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.Paragraph;

import java.awt.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Moris on 13.2.2017.
 */
public class RichTextFXHelpers {

    public static void addParagraphStyle(StyleClassedTextArea ta, int paragraphIndex, String style){
        Collection<String> styles = ta.getParagraph(paragraphIndex).getParagraphStyle().stream().collect(Collectors.toList());
        styles.add(style);
        ta.setParagraphStyle(paragraphIndex, styles);
    }

    public static void tryRemoveParagraphStyle(StyleClassedTextArea ta, int paragraphIndex, String style){
        //zober vsetky styly, ktore su aplikovane na paragraf okrem odoberaneho stylu
        java.util.List<String> styles = ta.getParagraph(paragraphIndex).getParagraphStyle().stream().filter(st-> !st.equals(style)).collect(Collectors.toList());
        //nastav vsetky okrem daneho stylu
        ta.setParagraphStyle(paragraphIndex, styles);
    }
}
