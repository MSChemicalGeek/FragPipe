/*
 *Connecting the python script to FragPipe. Sep 27th, 2023. Carolina Rojas Ramirez.
 * This file is part of FragPipe.
 *
 * FragPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FragPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FragPipe. If not, see <https://www.gnu.org/licenses/>.
 */

package com.dmtavt.fragpipe.tools.tpp;

import static com.dmtavt.fragpipe.tabs.TabConfig.pythonMinVersion;

import com.dmtavt.fragpipe.FragpipeLocations;
import com.dmtavt.fragpipe.api.Bus;
import com.dmtavt.fragpipe.api.PyInfo;
import com.dmtavt.fragpipe.exceptions.ValidationException;
import com.dmtavt.fragpipe.messages.*;
import com.github.chhh.utils.Installed;
import com.github.chhh.utils.PythonModule;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TppScript {
    private static final Logger log = LoggerFactory.getLogger(TppScript.class);
    private static TppScript INSTANCE = new TppScript();
    private final Object initLock = new Object();
    public static TppScript get() { return INSTANCE; }

    public static final String TPP_SCRIPT_PATH = "tpp/TPP-FragPipeDownstream.py";
    public static final String[] RESOURCE_LOCATIONS = {TPP_SCRIPT_PATH};
    public static final List<PythonModule> REQUIRED_MODULES = Arrays.asList(PythonModule.NUMPY, PythonModule.PANDAS);

    private PyInfo pi;
    /*Previously scriptFpopQuant*/
    private Path scriptTPP;
    private boolean isInitialized;

    /** To be called by top level application in order to initialize
     * the singleton and subscribe it to the bus. */
    public static void initClass() {
        log.debug("Static initialization initiated");
        TppScript o = new TppScript();
        Bus.register(o);
        TppScript.INSTANCE = o;
    }

    private TppScript() {
        pi = null;
        scriptTPP = null;
        isInitialized = false;
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN_ORDERED)
    public void on(NoteConfigPython m) {
        onPythonChange(m);
    }

    private void onPythonChange(NoteConfigPython python) {
        try {
            log.debug("Started init of: {}, python null={}", TppScript.class.getSimpleName(), python == null);
            init(python);
            Bus.postSticky(new NoteConfigTpp(this, null));
        } catch (ValidationException e) {
            Bus.postSticky(new NoteConfigTpp(null, e));
        }
    }

    public Path getScriptTpp() {
        return scriptTPP;
    }

    public PyInfo getPythonInfo() {
        return pi;
    }

    public boolean isInitialized() {
        synchronized (initLock) {
            return isInitialized;
        }
    }

    private void init(NoteConfigPython python) throws ValidationException {
        synchronized (initLock) {
            if (python == null || python.pi == null)
                throw new ValidationException("Python needs to be configured first.");
            isInitialized = false;

            checkPython(python);
            validateAssets();

            isInitialized = true;
            log.debug("{} init complete",TppScript.class.getSimpleName());
        }
    }

    private void checkPython(NoteConfigPython m) throws ValidationException {
        checkPythonVer(m);
        checkPythonModules(m.pi);
        this.pi = m.pi;
    }

    private void checkPythonModules(PyInfo pi) throws ValidationException {
        Map<Installed, List<PythonModule>> modules = pi.modulesByStatus(REQUIRED_MODULES);
        final Map<Installed, String> bad = new LinkedHashMap<>();
        bad.put(Installed.NO, "Missing");
        bad.put(Installed.INSTALLED_WITH_IMPORTERROR, "Error loading module");
        bad.put(Installed.UNKNOWN, "N/A");

        if (modules.keySet().stream().anyMatch(bad::containsKey)) {
            final List<String> byStatus = new ArrayList<>();
            for (Installed status : bad.keySet()) {
                List<PythonModule> list = modules.get(status);
                if (list != null) {
                    byStatus.add(bad.get(status) + " - " + list.stream().map(pm -> pm.installName).collect(Collectors.joining(", ")));
                }
            }
            throw new ValidationException("Python modules: " + String.join(", ", byStatus));
        }

        this.pi = pi;
    }

    private void checkPythonVer(NoteConfigPython m) throws ValidationException {
        if (m.pi == null || m.pi.getFullVersion().compareTo(pythonMinVersion) < 0) {
            throw new ValidationException("Python version " + pythonMinVersion + "+ is required");
        }
    }

    private void validateAssets() throws ValidationException {
        try {
            List<Path> paths = FragpipeLocations.tryLocateTools(Seq.of(RESOURCE_LOCATIONS)
                    .map(loc -> loc.startsWith("/") ? loc.substring(1) : loc));
            final String scriptTppFn = Paths.get(TPP_SCRIPT_PATH).getFileName().toString();
            Optional<Path> mainScript = paths.stream()
                    .filter(p -> p.getFileName().toString().equalsIgnoreCase(scriptTppFn))
                    .findFirst();
            if (!mainScript.isPresent()) {
                throw new ValidationException("Could not determine location of TPP python script " + TPP_SCRIPT_PATH);
            }
            scriptTPP = mainScript.get();
        } catch (MissingAssetsException e) {
            log.error("TPP script is missing assets in tools folder:\n{}", Seq.seq(e.getNotExisting()).toString("\n"));
            String missingRelativePaths = Seq.seq(e.getNotExisting())
                    .map(p -> FragpipeLocations.get().getDirTools().relativize(p))
                    .map(Path::toString).toString("; ");
            throw new ValidationException("Missing assets in tools/ folder:\n" + missingRelativePaths, e);
        }
    }

}
