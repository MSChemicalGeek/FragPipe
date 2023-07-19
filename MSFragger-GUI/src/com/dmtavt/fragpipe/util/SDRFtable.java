package com.dmtavt.fragpipe.util;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class SDRFtable {

    private ArrayList<String[]> rows;
    private ArrayList<String> header;
    private int firstEnzymeIndex;
    private int firstModIndex;

    // column names
    private final String COL_source = "source name";
    private final String COL_organism = "characteristics[organism]";
    private final String COL_strain = "characteristics[strain/breed]";
    private final String COL_cultivar = "characteristics[ecotype/cultivar]";
    private final String COL_ancestry = "characteristics[ancestry category]";
    private final String COL_age = "characteristics[age]";
    private final String COL_stage = "characteristics[developmental stage]";
    private final String COL_sex = "characteristics[sex]";
    private final String COL_disease = "characteristics[disease]";
    private final String COL_part = "characteristics[organism part]";
    private final String COL_cellType = "characteristics[cell type]";
    private final String COL_indvidual = "characteristics[individual]";
    private final String COL_cellLine = "characteristics[cell line]";
    private final String COL_bioReplicate = "characteristics[biological replicate]";
    private final String COL_technology = "technology type";
    private final String COL_assay = "assay name";
    private final String COL_datafile = "comment[data file]";
    private final String COL_replicate = "comment[technical replicate]";
    private final String COL_fraction = "comment[fraction identifier]";
    private final String COL_label = "comment[label]";
    private final String COL_instrument = "comment[instrument]";
    private final String COL_enzyme = "comment[cleavage agent details]";
    private final String COL_mods = "comment[modification parameters]";
    private final String COL_precTol = "comment[precursor mass tolerance]";
    private final String COL_prodTol = "comment[fragment mass tolerance]";

    public SDRFtable(SDRFtypes type, int numEnzymes, int numMods) {
        rows = new ArrayList<>();
        header = new ArrayList<>();

        // set up header for general sample characteristics
        initHeaderCharacteristics(type);

        // headers for lcms run/search information
        header.add(COL_datafile);
        header.add(COL_replicate);
        header.add(COL_fraction);
        header.add(COL_label);
        header.add(COL_instrument);
        header.add(COL_precTol);
        header.add(COL_prodTol);

        // support multiple enzymes and mods, each in their own column
        firstEnzymeIndex = header.size();
        for (int i=0; i < numEnzymes; i++){
            header.add(COL_enzyme);
        }
        firstModIndex = header.size();
        for (int i=0; i < numMods; i++){
            header.add(COL_mods);
        }
    }

    /**
     * Header setup for various types of SDRF files.
     * @param type type of template to generate
     */
    private void initHeaderCharacteristics(SDRFtypes type) {
        header.add(COL_source);
        header.add(COL_organism);
        switch (type) {
            case Human:
                header.add(COL_ancestry);
                header.add(COL_age);
                header.add(COL_stage);
                header.add(COL_sex);
                header.add(COL_indvidual);
                break;
            case Vertebrates:
                header.add(COL_age);
                header.add(COL_stage);
                header.add(COL_sex);
                header.add(COL_indvidual);
                break;
            case NonVertebrates:
                header.add(COL_strain);
                header.add(COL_age);
                header.add(COL_stage);
                header.add(COL_sex);
                header.add(COL_indvidual);
            case Plants:
                header.add(COL_cultivar);
                header.add(COL_age);
                header.add(COL_stage);
                header.add(COL_indvidual);
            case CellLines:
                header.add(COL_cellLine);
        }
        header.add(COL_disease);
        header.add(COL_part);
        header.add(COL_cellType);
        header.add(COL_bioReplicate);
        header.add(COL_technology);
        header.add(COL_assay);
    }

    /**
     * Add a row for an unlabeled sample (single LC-MS file)
     */
    public void addSampleLFQ(String lcmsfileName, String replicate, ArrayList<String> enzymes, ArrayList<String> mods) {
        String[] row = new String[header.size()];
        row[header.indexOf(COL_datafile)] = lcmsfileName;
        row[header.indexOf(COL_replicate)] = replicate;
        for (int i=0; i < enzymes.size(); i++) {
            row[firstEnzymeIndex + i] = enzymes.get(i);
        }
        for (int i=0; i < mods.size(); i++) {
            row[firstModIndex + i] = mods.get(i);
        }
        rows.add(row);
    }

    public void printTable(Path path) throws IOException {
        ArrayList<String> outputLines = new ArrayList<>();
        outputLines.add(String.join("\t", header));
        for (String[] row : rows) {
            outputLines.add(String.join("\t", row));
        }
        FileUtils.write(path.toFile(), String.join("\n", outputLines), StandardCharsets.UTF_8, false);
    }

    public enum SDRFtypes {
        Default("default"),
        Human("human"),
        CellLines("cell-lines"),
        Vertebrates("vertebrates"),
        NonVertebrates("non-vertebrates"),
        Plants("plants");

        private final String text;
        SDRFtypes(String _text) {
            this.text = _text;
        }
        public String getText() {
            return this.text;
        }
    }

}
