package com.dmtavt.fragpipe.tools.skyline;

import static com.dmtavt.fragpipe.tools.skyline.Skyline.getSkylineVersion;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WriteSky {

  private static final Pattern p = Pattern.compile("([\\d.-]+),([^,]+),(true),[\\d-]+;");
  private static final Pattern p1 = Pattern.compile("[n\\[](.)");
  private static final Pattern p2 = Pattern.compile("[c\\]](.)");
  private static final Pattern p3 = Pattern.compile("([\\d.-]+)\\((aa=([^=_();]+)?)?(_d=([\\d., -]+))?(_p=([\\d., -]+))?(_f=([\\d., -]+))?\\)");


  public WriteSky(Path path, String fixModStr, String varModStr, String massOffsetStr, String detailedMassOffsetStr) throws Exception {
    List<Mod> mods = new ArrayList<>(4);

    float mass;
    Matcher m;
    if (fixModStr != null && !fixModStr.isEmpty()) {
      m = p.matcher(fixModStr);
      while (m.find()) {
        if (m.group(3).equalsIgnoreCase("true")) {
          mass = Float.parseFloat(m.group(1));
          if (Math.abs(mass) > 0.1) {
            mods.addAll(convertMods(m.group(2), false, mass, mass, new ArrayList<>(0), new ArrayList<>(0)));
          }
        }
      }
    }

    if (varModStr != null && !varModStr.isEmpty()) {
      m = p.matcher(varModStr);
      while (m.find()) {
        if (m.group(3).equalsIgnoreCase("true")) {
          mass = Float.parseFloat(m.group(1));
          if (Math.abs(mass) > 0.1) {
            mods.addAll(convertMods(m.group(2), true, mass, mass, new ArrayList<>(0), new ArrayList<>(0)));
          }
        }
      }
    }

    if (massOffsetStr != null && !massOffsetStr.isEmpty()) {
      String[] ss = massOffsetStr.split("[\\s/]");
      for (String s : ss) {
        mass = Float.parseFloat(s);
        if (Math.abs(mass) > 0.1) {
          mods.addAll(convertMods("*", true, mass, mass, new ArrayList<>(0), new ArrayList<>(0)));
        }
      }
    }

    if (detailedMassOffsetStr != null && !detailedMassOffsetStr.isEmpty()) {
      m = p3.matcher(detailedMassOffsetStr);
      while(m.find()) {
        mass = Float.parseFloat(m.group(1));
        if (Math.abs(mass) > 0.1) {
          String sites = "*";
          List<Float> lossMonoMasses = new ArrayList<>(0);
          List<Float> lossAvgMasses = new ArrayList<>(0);
          if (m.group(3) != null) {
            sites = m.group(3);
          }
          if (m.group(9) != null) {
            String[] ss = m.group(9).split("[, ]+");
            for (String s : ss) {
              float reminderMass = Float.parseFloat(s);
              lossMonoMasses.add(mass - reminderMass);
              lossAvgMasses.add(mass - reminderMass);
            }
          }
          mods.addAll(convertMods(sites, true, mass, mass, lossMonoMasses, lossAvgMasses));
        }
      }
    }

    BufferedWriter bw = new BufferedWriter(Files.newBufferedWriter(path));
    bw.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
        + "<srm_settings format_version=\"23.1\" software_version=\"Skyline (64-bit) " + getSkylineVersion() + "\">\n"
        + "  <settings_summary name=\"Extra Mods\">\n"
        + "    <peptide_settings>\n"
        + "      <peptide_modifications>\n"
        + "        <static_modifications>\n");

    for (Mod mod : mods) {
      bw.write("          <static_modification name=\"" + mod.name + (mod.aa.isEmpty() ? "" : "\" aminoacid=\"" + mod.aa) + (mod.terminus == '\0' ? "" : "\" terminus=\"" + mod.terminus) + "\" variable=\"" + mod.isVariable + "\" massdiff_monoisotopic=\"" + mod.monoMass + "\" massdiff_average=\"" + mod.avgMass + "\">\n");
      for (int i = 0; i < mod.lossMonoMasses.size(); i++) {
        bw.write("            <potential_loss massdiff_monoisotopic=\"" + mod.lossMonoMasses.get(i) + "\" massdiff_average=\"" + mod.lossAvgMasses.get(i) + "\" />\n");
      }
      bw.write("          </static_modification>\n");
    }

    bw.write("        </static_modifications>\n"
        + "      </peptide_modifications>\n"
        + "    </peptide_settings>\n"
        + "  </settings_summary>\n"
        + "</srm_settings>\n");

    bw.close();
  }

  static List<Mod> convertMods(String sites, boolean isVariable, float monoMass, float avgMass, List<Float> lossMonoMasses, List<Float> lossAvgMasses) {
    List<Mod> out = new ArrayList<>(4);

    if (sites.contains(" ")) {
      sites = sites.substring(0, sites.indexOf(" "));
    }

    Matcher m = p1.matcher(sites);
    while (m.find()) {
      if (m.group(1).contentEquals("^")) {
        out.add(new Mod("n_" + monoMass, "", 'N', isVariable, monoMass, avgMass, lossMonoMasses, lossAvgMasses));
      } else if (m.group(1).contentEquals("*")) {
        for (char aa : "ACDEFGHIKLMNPQRSTVWY".toCharArray()) {
          out.add(new Mod(aa + "_" + monoMass, aa + "", 'N', isVariable, monoMass, avgMass, lossMonoMasses, lossAvgMasses));
        }
      } else {
        out.add(new Mod(m.group(1) + "_" + monoMass, m.group(1), 'N', isVariable, monoMass, avgMass, lossMonoMasses, lossAvgMasses));
      }
    }
    sites = p1.matcher(sites).replaceAll("");

    m = p2.matcher(sites);
    while (m.find()) {
      if (m.group(1).contentEquals("^")) {
        out.add(new Mod("c_" + monoMass, "", 'C', isVariable, monoMass, avgMass, lossMonoMasses, lossAvgMasses));
      } else if (m.group(1).contentEquals("*")) {
        for (char aa : "ACDEFGHIKLMNPQRSTVWY".toCharArray()) {
          out.add(new Mod(aa + "_" + monoMass, aa + "", 'C', isVariable, monoMass, avgMass, lossMonoMasses, lossAvgMasses));
        }
      } else {
        out.add(new Mod(m.group(1) + "_" + monoMass, m.group(1), 'C', isVariable, monoMass, avgMass, lossMonoMasses, lossAvgMasses));
      }
    }
    sites = p2.matcher(sites).replaceAll("");

    for (char c : sites.toCharArray()) {
      if (c == '*') {
        for (char aa : "ACDEFGHIKLMNPQRSTVWY".toCharArray()) {
          out.add(new Mod(aa + "_" + monoMass, aa + "", '\0', isVariable, monoMass, avgMass, lossMonoMasses, lossAvgMasses));
        }
      } else {
        out.add(new Mod(c + "_" + monoMass, c + "", '\0', isVariable, monoMass, avgMass, lossMonoMasses, lossAvgMasses));
      }
    }

    return out;
  }


  static class Mod {

    public final String name;
    public final String aa;
    public final char terminus;
    public final boolean isVariable;
    public final float monoMass;
    public final float avgMass;
    public final List<Float> lossMonoMasses;
    public final List<Float> lossAvgMasses;

    Mod(String name, String aa, char terminus, boolean isVariable, float monoMass, float avgMass, List<Float> lossMonoMasses, List<Float> lossAvgMasses) {
      this.name = name;
      this.aa = aa;
      this.terminus = terminus;
      this.isVariable = isVariable;
      this.monoMass = monoMass;
      this.avgMass = avgMass;
      this.lossMonoMasses = lossMonoMasses;
      this.lossAvgMasses = lossAvgMasses;
    }

    public String toString() {
      return name + " " + aa + " " + terminus + " " + isVariable + " " + monoMass + " " + avgMass + " " + lossMonoMasses + " " + lossAvgMasses;
    }
  }
}
