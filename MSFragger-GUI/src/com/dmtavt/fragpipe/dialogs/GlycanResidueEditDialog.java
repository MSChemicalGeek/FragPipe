package com.dmtavt.fragpipe.dialogs;

import com.dmtavt.fragpipe.Fragpipe;
import com.dmtavt.fragpipe.api.GlycoResiduesTable;
import com.dmtavt.fragpipe.api.GlycoResiduesTableModel;
import com.dmtavt.fragpipe.cmd.ToolingUtils;
import umich.ms.glyco.GlycanResidue;
import com.github.chhh.utils.swing.renderers.TableCellDoubleRenderer;
import com.github.chhh.utils.swing.renderers.TableCellIntRenderer;
import com.github.chhh.utils.swing.renderers.TableCellIntSpinnerEditor;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.List;

public class GlycanResidueEditDialog extends javax.swing.JDialog {
    private static final Logger log = LoggerFactory.getLogger(GlycanResidueEditDialog.class);
    private List<? extends GlycanResidue> initResidues;

    private JPanel p;
    private JButton buttonOK;
    private JButton buttonCancel;
    private GlycoResiduesTableModel model;
    public GlycoResiduesTable table;
    private Frame parent;
    private int dialogResult = JOptionPane.CLOSED_OPTION;
    private static final String[] TABLE_COL_NAMES = {"Name", "Mass", "Alternate Names (optional)",
            "is labile?", "Y prob +", "Y prob -", "Elemental Composition"};
    public static final String TAB_PREFIX = "glycan-database.";

    public GlycanResidueEditDialog(java.awt.Frame parent, List<? extends GlycanResidue> initialResidues) {
        super(parent);
        this.initResidues = initialResidues == null ? Collections.emptyList() : initialResidues;
        this.parent = parent;
        init();
        postInit();
    }

    public GlycoResiduesTableModel getModel() {
        return model;
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    private void postInit() {
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setLocationRelativeTo(parent);
        pack();
    }

    private void init() {
        Dimension dim = new Dimension(800, 600);
        this.setPreferredSize(dim);
        this.setLayout(new BorderLayout());

        p = new JPanel();
        JScrollPane scroll = new JScrollPane(p,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.setContentPane(scroll);

        buttonOK = new JButton("Save");
        buttonCancel = new JButton("Cancel");
        table = createTable();

        MigLayout layout = new MigLayout(new LC().fillX());//.debug());
        p.setLayout(layout);
        p.add(new JScrollPane(table), new CC().grow().spanX().wrap());
        p.add(buttonOK, new CC().tag("ok").split());
        p.add(buttonCancel, new CC().tag("cancel").wrap());

        setContentPane(scroll);
        setModal(true);
        setModalityType(ModalityType.APPLICATION_MODAL);
        setTitle("Edit Glycan Residue Definitions:");
        setIconImages(ToolingUtils.loadIcon());
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(e -> onOK());
        buttonCancel.addActionListener(e -> onCancel());
        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });
        // call onCancel() on ESCAPE
        p.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {
        // add your code here
        dialogResult = JOptionPane.OK_OPTION;
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dialogResult = JOptionPane.CANCEL_OPTION;
        dispose();
    }

    public int getDialogResult() {
        return dialogResult;
    }

    private GlycoResiduesTable createTable() {
        Object[][] data = GlycoResiduesTableModel.convertGlycoResiduesToData(initResidues);
        model = new GlycoResiduesTableModel(
                TABLE_COL_NAMES,
                new Class<?>[]{String.class, Double.class, String.class, Boolean.class, Double.class, Double.class, String.class},
                new boolean[]{true, true, true, true, true, true, true, true, true},
                data);
        final GlycoResiduesTable t = new GlycoResiduesTable(model, TABLE_COL_NAMES, GlycoResiduesTableModel::convertGlycoResiduesToData);
        Fragpipe.rename(t, "table.glyco-residues", TAB_PREFIX);

        t.setToolTipText("<html>Edit the glycan residue definitions (also possible by editing the file manually).<br/>\n" +
                "Name: required - the name of the glycan residue in glycan databases that will be loaded<br/>\n" +
                "Mass: required - the monoisotopic mass of the glycan residue<br/>\n" +
                "Alternate Names: optional - other names that the glycan might be called<br/>\n" +
                "is labile?: optional - if the residue is expected to be lost from fragment ions<br/>\n" +
                "Y prob +: required unless labile - empirical score factor for finding Y ions. Default 5<br/>\n" +
                "Y prob -: required unless labile - empirical score factor for missing Y ions. Default 0.5<br/>\n" +
                "Elemental Composition: optional - used for Skyline conversion, not required for searches.");
        t.setDefaultRenderer(Float.class, new TableCellDoubleRenderer());
        t.setDefaultRenderer(Integer.class, new TableCellIntRenderer());

        // set cell editor for max occurs for var mods
        DefaultCellEditor cellEditorMaxOccurs = new TableCellIntSpinnerEditor(0, 5, 1);
        t.setDefaultEditor(Integer.class, cellEditorMaxOccurs);
        t.setFillsViewportHeight(true);

        return t;
    }




}