package com.dmtavt.fragpipe.tools.crystalc;

import com.github.chhh.utils.swing.JPanelBase;
import com.github.chhh.utils.swing.MigUtils;
import com.github.chhh.utils.swing.UiCheck;
import com.github.chhh.utils.swing.UiSpinnerDouble;
import com.github.chhh.utils.swing.UiSpinnerInt;
import java.awt.Component;
import java.awt.ItemSelectable;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.miginfocom.layout.CC;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.dmtavt.fragpipe.messages.MessageLoadCrystalcDefaults;
import com.dmtavt.fragpipe.messages.MessageSearchType;
import com.github.chhh.utils.SwingUtils;
import com.github.chhh.utils.swing.FormEntry;

public class CrystalcPanel extends JPanelBase {
  private static final Logger log = LoggerFactory.getLogger(CrystalcPanel.class);
  private static final MigUtils mu = MigUtils.get();
  public JCheckBox checkRun;
  private JPanel pTop;
  private JPanel pParams;
  private UiSpinnerInt uiSpinnerIntMaxCharge;
  private UiSpinnerInt uiSpinnerIntNumIsotopes;
  private UiSpinnerInt uiSpinnerIntMassTolPpm;
  private UiCheck uiCheckCorrectIsoErr;
  private UiSpinnerDouble uiSpinnerPrecIsol;
  public static final String PREFIX = "crystalc.";

  @Override
  protected ItemSelectable getRunCheckbox() {
    return checkRun;
  }

  @Override
  protected Component getEnablementToggleComponent() {
    return pParams;
  }

  @Override
  protected String getComponentNamePrefix() {
    return PREFIX;
  }

  @Override
  protected void init() {
    pTop = createPanelTop();
    pParams = createPanelParams();

    mu.layout(this);
    mu.border(this, "Crystal-C");
    mu.add(this, pTop).growX().wrap();
    mu.add(this, pParams).growX().wrap();
  }

  @Override
  protected void initMore() {
    updateEnabledStatus(pParams, SwingUtils.isEnabledAndChecked(checkRun));
    super.initMore();
  }

  private void loadDefaults() {
    try {
      final Map<String, String> newVals = new HashMap<>();
      final CrystalcParams props = new CrystalcParams();
      props.loadDefault();
      props.getProps().getMap().keySet().forEach(key -> {
        newVals.put(PREFIX + key, props.getProps().getProp(key).value);
      });
      newVals.put(PREFIX + "max-charge", Integer.toString(props.getMaxZ()));
      Map<String, String> oldVals = SwingUtils.valuesGet(this);
      log.debug("Loading defaults for crystalc, iterating keys in newly loaded defaults");
      for (String k : newVals.keySet()) {
        String newVal = newVals.get(k);
        String oldVal = oldVals.get(k);
        log.debug("Default key={}. Old val: {}, new val: {}", k, oldVal, newVal);
      }
      List<String> oldKeysNotInNewDefaults = oldVals.keySet().stream().filter(k -> !newVals.containsKey(k))
          .sorted().collect(Collectors.toList());
      if (!oldKeysNotInNewDefaults.isEmpty()) {
        log.debug("Found old keys without new defaults: {}", oldKeysNotInNewDefaults);
      }
      SwingUtils.valuesSet(this, newVals);
    } catch (Exception e) {
      log.error("Error loading Crystal-C defaults", e);
      SwingUtils.showErrorDialogWithStacktrace(e, this);
    }
  }

  public CrystalcParams toParams() {
    CrystalcParams p = new CrystalcParams();
    p.loadDefault();
    p.setMaxZ(uiSpinnerIntMaxCharge.getActualValue());
    p.setIsoNum(uiSpinnerIntNumIsotopes.getActualValue());
    p.setPrecursorMassTol(uiSpinnerIntMassTolPpm.getActualValue());
    p.setPrecursorIsolationWindow((Double)uiSpinnerPrecIsol.getValue());
    p.setCorrectIsotopeError(uiCheckCorrectIsoErr.isSelected());
    return p;
  }

  public boolean isRun() {
    return SwingUtils.isEnabledAndChecked(checkRun);
  }

  @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
  public void on(MessageLoadCrystalcDefaults m) {
    if (m.doAskUser) {
      int answer = SwingUtils.showConfirmDialog(this, new JLabel("<html>Load Crystal-C defaults?"));
      if (JOptionPane.OK_OPTION != answer) {
        log.debug("User cancelled Loading Crystal-C defaults");
        return;
      }
    }

    loadDefaults();
  }

  @Subscribe
  public void on(MessageSearchType m) {
    switch (m.type) {
      case open:
        checkRun.setSelected(true);
        break;
      case closed:
      case nonspecific:
      case offset:
        checkRun.setSelected(false);
        break;
    }
  }

  private JPanel createPanelTop() {
    // setting the insets allows the top panel to be shifted left of the options panel
    JPanel pTop = mu.newPanel(null, mu.lcFillXNoInsetsTopBottom());

    checkRun = new UiCheck("Run Crystal-C", null, false);
    checkRun.setName("run-crystalc");
    checkRun.addActionListener(e -> {
      final boolean isSelected = checkRun.isSelected();
      enablementMapping.put(pParams, isSelected);
      updateEnabledStatus(pParams, isSelected);
    });
    checkRun.addChangeListener(e -> {
      final boolean isSelected = checkRun.isSelected();
      enablementMapping.put(pParams, isSelected);
      updateEnabledStatus(pParams, isSelected);
    });


    pTop.add(checkRun, new CC().alignX("left"));
    JButton btnLoadDefaults = new JButton("Load Crystal-C defaults");
    btnLoadDefaults.addActionListener((e) -> EventBus
        .getDefault().post(new MessageLoadCrystalcDefaults(true)));
    pTop.add(btnLoadDefaults, new CC().alignX("left"));
    final String htmlTooltip = "<html>Crystal-C performs additional search results cleanup<br/>\n"
        + "Recommended for Open Searches only";
    JLabel info = new JLabel(htmlTooltip);
    info.setHorizontalAlignment(SwingConstants.LEADING);
    info.setVerticalAlignment(SwingConstants.TOP);
    info.setAlignmentY(Component.TOP_ALIGNMENT);
    info.setVerticalTextPosition(SwingConstants.TOP);
    pTop.add(info, new CC().gapBefore("50px").alignX("right"));
    pTop.setBorder(new EmptyBorder(0,0,0,0));

    return pTop;
  }

  private JPanel createPanelParams() {
    uiSpinnerIntMaxCharge = new UiSpinnerInt(6, 1, 50, 1, 5);
    FormEntry feMaxCharge = new FormEntry("max-charge", "Max charge",
        uiSpinnerIntMaxCharge);
    uiSpinnerIntNumIsotopes = new UiSpinnerInt(3, 1, 50, 1, 5);
    FormEntry feNumIsotopes = new FormEntry(CrystalcParams.PROP_isotope_number,
        "Number of isotopes", uiSpinnerIntNumIsotopes);
    uiSpinnerIntMassTolPpm = new UiSpinnerInt(20, 1, 1000, 1, 5);
    FormEntry feMassTolPpm = new FormEntry(CrystalcParams.PROP_precursor_mass , "Mass tolerance (ppm)",
        uiSpinnerIntMassTolPpm);
    uiSpinnerPrecIsol = UiSpinnerDouble.builder(0.7,0.0,1000.0, 0.1)
        .setFormat(new DecimalFormat("0.#")).setNumCols(5).create();
    FormEntry fePrecIsol = new FormEntry(CrystalcParams.PROP_precursor_isolation_window, "Precursor isolation window",
        uiSpinnerPrecIsol);
    uiCheckCorrectIsoErr = new UiCheck("Correct isotope error", null, false);
    FormEntry feCheckCorrectIsoErr = new FormEntry(CrystalcParams.PROP_correct_isotope_error, "not-shown",
        uiCheckCorrectIsoErr,
        "Correct isotope error by updating precursor neutral mass with the monoisotopic mass");

    JPanel p = mu.newPanel(null, mu.lcFillXNoInsetsTopBottom());
    p.add(feMaxCharge.label(), new CC().alignX("right"));
    p.add(feMaxCharge.comp, new CC().alignX("left"));
    p.add(feNumIsotopes.label(), new CC().alignX("right"));
    p.add(feNumIsotopes.comp, new CC().alignX("left"));
    p.add(feCheckCorrectIsoErr.comp, new CC().alignX("left").wrap());
    p.add(feMassTolPpm.label(), new CC().alignX("right"));
    p.add(feMassTolPpm.comp, new CC().alignX("left"));
    p.add(fePrecIsol.label(), new CC().alignX("right"));
    p.add(fePrecIsol.comp, new CC().alignX("left").wrap());

    return p;
  }
}
