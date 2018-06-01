/*
    plugin : ParticleDensityDouble_.java
    author : Max Larsson
    e-mail : max.larsson@liu.se

    This ImageJ plugin is for use in conjunction with PointDensity.py.

    Copyright 2001-2014 Max Larsson <max.larsson@liu.se>

    This software is released under the MIT license.
      
*/

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Random;
import ij.*;
import ij.IJ;
import ij.ImagePlus;
import ij.io.*;
import ij.gui.*;
import ij.plugin.frame.*;
import ij.measure.*;

interface VersionPDD {
    String title = "ParticleDensityDouble";
    String author = "Max Larsson";
    String version = "1.0pre3";
    String year = "2015";
    String month = "April";
    String day = "21";
    String email = "max.larsson@liu.se";
    String homepage = "http://www.hu.liu.se/forskning/larsson-max/software";
}

interface OptionsPDD {
    Color profileCol = Color.blue;
    Color smallParticleCol = Color.green;
    Color largeParticleCol = Color.orange;
    Color randomCol = Color.yellow;
    Color holeCol = Color.red;
    Color textCol = Color.blue;
}


public class ParticleDensityDouble_ extends PlugInFrame implements OptionsPDD, ActionListener {
    
    Panel panel;
    static Frame instance;
    static Frame infoFrame;
    GridBagLayout infoPanel;
    GridBagConstraints c;
    Label profile_nLabel;
    Label spnLabel;
    Label lpnLabel;   
    Label pathnLabel;    
    Label holenLabel;        
    Label randomPlacedLabel;
    Label commentLabel;
    Label scaleLabel;
    static Choice profileType;
    ProfileDataPDD profile;
    ImagePlus imp;
    double wRandomROI=1.0, hRandomROI=1.0;

    public ParticleDensityDouble_() {
        super("ParticleDensityDouble");
        if (instance != null) {
            instance.toFront();
            return;
        }
        instance = this;
        profile = new ProfileDataPDD();
        IJ.register(ParticleDensityDouble_.class);
        setLayout(new FlowLayout());
        setBackground(SystemColor.control);
        panel = new Panel();
        panel.setLayout(new GridLayout(0, 1, 4, 1));
        panel.setBackground(SystemColor.control);    
        panel.setFont(new Font("Helvetica", 0, 12));
        addButton("Save profile");
        addButton("Clear profile");
        panel.add(new Label(""));
        panel.add(new Label("Define selection as:"));
        addButton("Profile border");
        addButton("Small particles");
        addButton("Large particles");        
        addButton("Hole");
        panel.add(new Label(""));        
        addButton("Place random points");
        addButton("Randomly place ROI");
		panel.add(new Label(""));
        panel.add(new Label("Profile type:"));        
        profileType = new Choice();
        profileType.add("Not specified");                
        panel.add(profileType);                
        panel.add(new Label(""));
        panel.add(new Label("Delete profile components:"));
        addButton("Delete profile border");
        addButton("Delete small particles");
        addButton("Delete large particles");
        addButton("Delete random points");
        addButton("Delete selected component");
        panel.add(new Label(""));
        panel.add(new Label("Other:"));                
        addButton("Add comment");
        addButton("Set profile n");     
        addButton("Define profile type");
        addButton("Define ROI size");
        addButton("Options...");
        addButton("About...");
        add(panel);
        pack();
        setVisible(true);
        infoFrame = new Frame("Profile info");
        infoPanel = new GridBagLayout();
        infoFrame.setFont(new Font("Helvetica", 0, 10));
        infoFrame.setBackground(SystemColor.control);
        infoFrame.setLocation(0, instance.getLocation().x + instance.getSize().height + 3);
        infoFrame.setIconImage(instance.getIconImage());
        infoFrame.setResizable(false);
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        addStaticInfoLabel("Profile n:");
        profile_nLabel = new Label(IJ.d2s(profile.ntot, 0), Label.RIGHT);
        addVarInfoLabel(profile_nLabel);
        addStaticInfoLabel("Small particles:");
        spnLabel = new Label("0", Label.RIGHT);
        addVarInfoLabel(spnLabel);
        addStaticInfoLabel("Large particles:");
        lpnLabel = new Label("0", Label.RIGHT);
        addVarInfoLabel(lpnLabel);
        addStaticInfoLabel("Profile border nodes:");
        pathnLabel = new Label("0", Label.RIGHT);
        addVarInfoLabel(pathnLabel);
        addStaticInfoLabel("Holes:");
        holenLabel = new Label("0", Label.RIGHT);
        addVarInfoLabel(holenLabel);
        addStaticInfoLabel("Random points:");
        randomPlacedLabel = new Label("no", Label.RIGHT);
        addVarInfoLabel(randomPlacedLabel);
        addStaticInfoLabel("Pixel width:");
        scaleLabel = new Label("N/D", Label.RIGHT);
        addVarInfoLabel(scaleLabel);
        addStaticInfoLabel("Comment:");
        commentLabel = new Label("", Label.RIGHT);
        addVarInfoLabel(commentLabel);
        infoFrame.setLayout(infoPanel);
        infoFrame.pack();
        infoFrame.setVisible(true);
        infoFrame.setSize(instance.getSize().width, infoFrame.getSize().height);
        instance.requestFocus();
    }
    
    void addButton(String label) {
        Button b = new Button(label);
        b.addActionListener(this);
        panel.add(b);
    }

    void addStaticInfoLabel(String label) {
        Label l = new Label(label, Label.LEFT);
        c.gridwidth = 1;
        infoPanel.setConstraints(l, c);
        infoFrame.add(l);
    }

    void addVarInfoLabel(Label l) {
        c.gridwidth = GridBagConstraints.REMAINDER;
        infoPanel.setConstraints(l, c);
        infoFrame.add(l);
    }
        
    PolygonRoi getPolygonRoi(ImagePlus imp) {
        Roi roi = imp.getRoi();
        if (roi == null || (roi.getType() != Roi.POLYGON && roi.getType() != Roi.RECTANGLE)) {
            IJ.error("ParticleDensityDouble", "Polygon or rectangle selection required.");
            return null;
        } else {
            return (PolygonRoi) roi;
        }
    }

    
    PolygonRoi getPointRoi(ImagePlus imp) {
        Roi roi = imp.getRoi();
        if (roi == null || roi.getType() != Roi.POINT) {
            IJ.error("ParticleDensityDouble", "Point selection required.");
            return null;
        } else {
            return (PolygonRoi) roi;
        }
    }

    void updateInfoPanel() {
        double pixelwidth;
        String unit;

        profile_nLabel.setText(IJ.d2s(profile.ntot, 0));
        spnLabel.setText(IJ.d2s(profile.getNumPoints("small particles"), 0));
        lpnLabel.setText(IJ.d2s(profile.getNumPoints("large particles"), 0));
        pathnLabel.setText(IJ.d2s(profile.getNumPoints("profile border"), 0));
        holenLabel.setText(IJ.d2s(profile.getNum("hole"), 0));
        if (profile.overlay.getIndex("random points") != -1) {
            randomPlacedLabel.setText("yes");
        } else {
            randomPlacedLabel.setText("no");
        }
        Calibration c = imp.getCalibration();
        if (c.getUnit().equals("micron")) {
            pixelwidth = c.pixelWidth * 1000;
            unit = "nm";
        } else {
            pixelwidth = c.pixelWidth;
            unit = c.getUnit();
        }
        scaleLabel.setText(IJ.d2s(pixelwidth, 4) + " " + unit);
        commentLabel.setText(profile.comment);
    }

    public boolean isImage(ImagePlus imp) {
        if (imp == null) {
            IJ.beep();
            IJ.showStatus("No image");
            return false;
        }
        return true;
    }

    public void actionPerformed(ActionEvent e) {
        PolygonRoi p;
        Polygon randomPol;
        PointRoi randomRoi;
        int i, x, y;
        int offsetx, offsety;
        int wpix, hpix;
        String s;

        String command = e.getActionCommand();
        if (command == null) {
            return;
        }
        imp = WindowManager.getCurrentImage();
        imp.setOverlay(profile.overlay);
        if (imp != null && imp.getType() != ImagePlus.COLOR_RGB) {
            imp.setProcessor(imp.getTitle(), imp.getProcessor().convertToRGB());
        }
        if (command.equals("Save profile")) {
            if (!isImage(imp)) {
                return;
            }
            if (!profile.dirty) {
                IJ.showMessage("Nothing to save.");
            } else {
                boolean saved = profile.save(imp);
                if (saved) {
                    profile.clear();
                }
            }
        }
        if (command.equals("Clear profile")) {
            if (!isImage(imp)) {
                return;
            }
            if (profile.dirty) {
                YesNoCancelDialog d = new YesNoCancelDialog(imp.getWindow(),
                        "ParticleDensityDouble", "Save current\nprofile?");
                if (d.yesPressed()) {
                    profile.dirty = !profile.save(imp);
                } else if (!d.cancelPressed()) {
                    profile.dirty = false;
                }
            }
            if (!profile.dirty) {
                profile.clear();
                IJ.showStatus("Profile cleared.");
            }
        }
        if (command.equals("Small particles")) {
            if (!isImage(imp) || !profile.isSameImage(imp) ||
                    profile.isDefined("small particles", "Small particles")) {
                return;
            }
            if ((p = getPointRoi(imp)) != null) {
                p.setName("small particles");
                p.setStrokeColor(smallParticleCol);
                profile.overlay.add(p);
                profile.dirty = true;
            }
        }
        if (command.equals("Large particles")) {
            if (!isImage(imp) || !profile.isSameImage(imp) ||
                    profile.isDefined("large particles", "Large particles")) {
                return;
            }
            if ((p = getPointRoi(imp)) != null) {
                p.setName("large particles");
                p.setStrokeColor(largeParticleCol);
                profile.overlay.add(p);
                profile.dirty = true;
            }
        }
        if (command.equals("Profile border")) {
            if (!isImage(imp) || !profile.isSameImage(imp) ||
                    profile.isDefined("profile border", "Profile border")) {
                return;
            }
            if ((p = getPolygonRoi(imp)) != null) {
                p.setName("profile border");
                p.setStrokeColor(profileCol);
                profile.overlay.add(p);
                profile.dirty = true;
            }
        }
        if (command.equals("Hole")) {
            if (!isImage(imp) || !profile.isSameImage(imp)) {
                return;
            }
            if ((p = getPolygonRoi(imp)) != null) {
                p.setName("hole");
                p.setStrokeColor(holeCol);
                profile.overlay.add(p);
                profile.dirty = true;
            }
        }
        if (command.equals("Place random points")) {
            if (!isImage(imp) || !profile.isSameImage(imp) ||
                    profile.overlay.getIndex("random points") != -1) {
                return;
            }
            Random rnd = new Random();
            randomPol = new Polygon();
            for (i = 0; i < profile.randompn; i++) {
                x = rnd.nextInt(imp.getWidth() - 1) + 1;
                y = rnd.nextInt(imp.getHeight() - 1) + 1;
                randomPol.addPoint(x, y);
            }
            randomRoi = new PointRoi(randomPol);
            randomRoi.setHideLabels(true);
            randomRoi.setName("random points");
            randomRoi.setStrokeColor(randomCol);
            profile.overlay.add(randomRoi);
        }
        if (command.equals("Randomly place ROI")) {
            Calibration c = imp.getCalibration();
            wpix = (int) java.lang.Math.floor(wRandomROI / c.pixelWidth);
            hpix = (int) java.lang.Math.floor(hRandomROI / c.pixelHeight);
            Random rnd = new Random();
            offsetx = rnd.nextInt(imp.getWidth() - wpix);
            offsety = rnd.nextInt(imp.getHeight() - hpix);
            imp.setRoi(offsetx, offsety, wpix, hpix);
        }
        if (command.equals("Define profile type")) {
            s = IJ.getString("Define profile type", "");
            if (!s.equals("")) {
                for (i = 0; i < profileType.getItemCount(); i++) {
                    if (s.equals(profileType.getItem(i))) {
                        YesNoCancelDialog d = new YesNoCancelDialog(imp.getWindow(),
                                "ParticleDensityDouble", "Profile type '" + s
                                + "' already defined. Remove?");
                        if (d.yesPressed()) {
                            profileType.remove(i);
                        }
                        return;
                    }
                }
                profileType.add(s);
            }
        }
        if (command.equals("Define ROI size")) {
            GenericDialog gd = new GenericDialog("Random ROI size");
            gd.setInsets(0, 0, 0);
            gd.addMessage("Specify sides in metric units:");
            gd.addNumericField("Width:", wRandomROI, 4);
            gd.addNumericField("Height:", hRandomROI, 4);
            gd.showDialog();
            if (gd.wasCanceled())
                return;
            wRandomROI = gd.getNextNumber();
            hRandomROI = gd.getNextNumber();
        }
        if (command.equals("Delete profile border")) {
            if (!isImage(imp) || !profile.isSameImage(imp)) {
                return;
            }
            profile.deleteNamedComponent(imp, "profile border");
        }
        if (command.equals("Delete small particles")) {
            if (!isImage(imp) || !profile.isSameImage(imp)) {
                return;
            }
            profile.deleteNamedComponent(imp, "small particles");
        }
        if (command.equals("Delete large particles")) {
            if (!isImage(imp) || !profile.isSameImage(imp)) {
                return;
            }
            profile.deleteNamedComponent(imp, "large particles");
        }
        if (command.equals("Delete random points")) {
            if (!isImage(imp) || !profile.isSameImage(imp)) {
                return;
            }
            profile.deleteNamedComponent(imp, "random points");
        }
        if (command.equals("Delete selected component")) {
            if ((imp.getRoi()) != null) {
                profile.deleteSelectedComponent(imp);
            }
        }
        if (command.equals("Set profile n")) {
            s = IJ.getString("Set profile n", IJ.d2s(profile.ntot, 0));
            profile.ntot = java.lang.Integer.parseInt(s);
        }
        if (command.equals("Add comment")) {
            s = IJ.getString("Comment: ", profile.comment);
            if (!s.equals("")) {
                profile.comment = s;
                profile.dirty = true;
            }
        }
        if (command.equals("Options...")) {
            GenericDialog gd = new GenericDialog("Options");
            gd.setInsets(0, 0, 0);
            gd.addMessage("Random particles:");
            gd.addNumericField("Random particle n:", profile.randompn, 0);
            gd.showDialog();
            if (gd.wasCanceled())
                return;
            profile.randompn = (int) gd.getNextNumber();
            if (profile.randompn <=0) {
                IJ.error("Random point n must be larger than 0. Reverting to default value (40).");
                profile.randompn = 40;
            }
        }
        if (command.equals("About...")) {
            String aboutHtml = String.format("<html><p><strong>%s" +
                            "</strong></p><br />" +
                            "<p>Version %s</p><br />" +
                            "<p>Last modified %s %s, %s.</p>" +
                            "<p> Copyright 2001 - %s %s.</p>" +
                            "<p> Released under the MIT license. " +
                            "</p><br />" +
                            "<p>E-mail: %s</p>" +
                            "<p>Web: %s</p><br /></html>",
                    VersionPDD.title,
                    VersionPDD.version,
                    VersionPDD.month,
                    VersionPDD.day,
                    VersionPDD.year,
                    VersionPDD.year,
                    VersionPDD.author,
                    VersionPDD.email,
                    VersionPDD.homepage);
            new HTMLDialog(VersionPDD.title, aboutHtml);
        }
        updateInfoPanel();
        imp.updateAndDraw();
        IJ.showStatus("");
    }

    public void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID()==WindowEvent.WINDOW_CLOSING) {
            infoFrame.dispose();
            infoFrame = null;
            instance = null;
        }
    }

} // end of ParticleDensityDouble_

class ProfileDataPDD implements OptionsPDD {
    boolean dirty;
    Overlay overlay;
    int n, ntot, randompn, i;
    int imgID;
    String ID, comment, prevImg;
    String profileType;

    ProfileDataPDD() {
        this.n = 0;
        this.ntot = 1;
        this.prevImg = "";
        this.imgID = 0;
        this.dirty = false;
        this.overlay = new Overlay();
        this.randompn = 200;
        this.profileType = "";
        this.comment = "";
        this.ID = "";
    }


    // Returns number of ROIs named 'name' in the overlay. Returns 0 if ROI not found in overlay.
    public int getNum(String name) {
        int i, n=0;

        for (i = 0; i < this.overlay.size(); i++) {
            if (this.overlay.get(i).getName().equals(name)) {
                n++;
            }
        }
        return n;
    }
    // Returns number of points in ROI named 'name' in the overlay. Returns 0 if ROI not found in overlay.
    public int getNumPoints(String name) {
        if (this.overlay.getIndex(name) == -1) {
            return 0;
        } else {
            return this.overlay.get(this.overlay.getIndex(name)).getPolygon().npoints;
        }
    }

    public void deleteSelectedComponent(ImagePlus imp) {
        if (!this.overlay.contains(imp.getRoi())) {
            IJ.error("The current selection does not define a profile component.");
        } else {
            YesNoCancelDialog d = new YesNoCancelDialog(imp.getWindow(),
                    "ParticleDensityDouble", "Delete " + imp.getRoi().getName() + "?");
            if (d.yesPressed()) {
                this.overlay.remove(imp.getRoi());
                imp.deleteRoi();
            }
        }
    }

    public void deleteNamedComponent(ImagePlus imp, String name) {
        if (this.overlay.getIndex(name) == -1) {
            IJ.error("No " + name + " defined.");
        } else {
            YesNoCancelDialog d = new YesNoCancelDialog(imp.getWindow(),
                    "ParticleDensityDouble", "Delete " + name + "?");
            if (d.yesPressed()) {
                this.overlay.remove(this.overlay.getIndex(name));
            }
        }
    }

    public boolean isSameImage(ImagePlus imp) {
        if (!this.dirty || this.imgID == 0) {
            this.imgID = imp.getID();
            return true;
        } else if (this.imgID == imp.getID()) {
            return true;
        } else {
            IJ.error("ParticleDensityDouble", "All measurements must be performed on the same image.");
            return false;
        }
    }

    public boolean isDefined(String name, String errstr) {
        if (this.overlay.getIndex(name) != -1) {
            IJ.error(errstr + " already defined. Please delete old instance first.");
            return true;
        }
        return false;
    }

    private boolean CheckProfileData(ImagePlus imp) {
        String[] warnstr, errstr;
        int i, nwarn = 0, nerr = 0;

        warnstr = new String[9];
        errstr = new String[9];
        Calibration c = imp.getCalibration();
        if (c.getUnit().equals(" ") || c.getUnit().equals("inch")) {
            errstr[nerr++] = "It appears the scale has not been set.";
        }
        if (this.getNumPoints("profile border") == 0) {
            errstr[nerr++] = "Profile border not defined.";
        }
        if (this.getNumPoints("small particles") == 0) {
            warnstr[nwarn++] = "No small particle coordinates defined.";
        }
        if (this.getNumPoints("large particles") == 0) {
            warnstr[nwarn++] = "No large particle coordinates defined.";
        }
        if (nerr > 0) {
            IJ.error("ParticleDensityDouble", "Error:\n" + errstr[0]);
            return false;
        }
        if (nwarn > 0) {
            for (i = 0; i < nwarn; i++) {
                YesNoCancelDialog d = new YesNoCancelDialog(imp.getWindow(),
                        "ParticleDensityDouble", "Warning:\n" + warnstr[i] + "\nContinue anyway?");
                if (!d.yesPressed()) {
                    return false;
                }
            }
        }
        return true;
    }


    public boolean save(ImagePlus imp) {
        int i, j;
        double pixelwidth;
        String s, unit;
        Polygon pol;

        IJ.showStatus("Saving profile...");
        if (!CheckProfileData(imp)) {
            return false;
        }
        Calibration c = imp.getCalibration();
        if (c.pixelWidth != c.pixelHeight) {
            IJ.showMessage("Warning: pixel aspect ratio is not 1.\n" +
                    "Only pixel WIDTH is used.");
        }
        try {
            if (!imp.getTitle().equals(this.prevImg)) {
                this.n = 0;
                this.prevImg = imp.getTitle();
            }
            this.n++;
            s = IJ.getString("Profile ID: ", IJ.d2s(this.ntot, 0));
            if (!s.equals("")) {
                this.ID = s;
            }
            SaveDialog sd = new SaveDialog("Save profile",
                    imp.getTitle() + "." +
                            IJ.d2s(this.n,0), ".pdd");
            if (sd.getFileName() == null) {
                this.n--;
                return false;
            }
            PrintWriter outf =
                    new PrintWriter(
                            new BufferedWriter(
                                    new FileWriter(sd.getDirectory() +
                                            sd.getFileName())));
            String versionInfo = String.format("# %s version %s (%s %s, %s)",
                    VersionPDD.title,
                    VersionPDD.version,
                    VersionPDD.month,
                    VersionPDD.day,
                    VersionPDD.year);
            outf.println(versionInfo);
            outf.println("IMAGE " + imp.getTitle());
            outf.println("PROFILE_ID " + this.ID);
            if (!this.comment.equals("")) {
                outf.println("COMMENT " + this.comment);
            }
            if (c.getUnit().equals("micron")) {
                pixelwidth = c.pixelWidth * 1000;
                unit = "nm";
            } else {
                pixelwidth = c.pixelWidth;
                unit = c.getUnit();
            }
            outf.println("PIXELWIDTH " + IJ.d2s(pixelwidth, 4) + " " + unit);
            outf.println("PROFILE_TYPE " + this.profileType);
            outf.println("PROFILE_BORDER");
            pol = this.overlay.get(this.overlay.getIndex("profile border")).getPolygon();
            for (i = 0; i < pol.npoints; i++) {
                outf.println("  " + IJ.d2s(pol.xpoints[i], 0) + ", "+ IJ.d2s(pol.ypoints[i], 0));
            }
            outf.println("END");
            for (n = 0; n < this.overlay.size(); n++) {
                if (this.overlay.get(n).getName().equals("hole")) {
                    outf.println("HOLE");
                    pol = this.overlay.get(n).getPolygon();
                    for (i = 0; i < pol.npoints; i++)
                        outf.println("  " + IJ.d2s(pol.xpoints[i], 0) + ", "+ IJ.d2s(pol.ypoints[i], 0));
                    outf.println("END");
                }
            }
            if (this.overlay.getIndex("small particles") != -1) {
                outf.println("SMALL_PARTICLES");
                pol = this.overlay.get(this.overlay.getIndex("small particles")).getPolygon();
                for (i = 0; i < pol.npoints; i++) {
                    outf.println("  " + IJ.d2s(pol.xpoints[i], 0) + ", " + IJ.d2s(pol.ypoints[i], 0));
                }
                outf.println("END");
            }
            if (this.overlay.getIndex("large particles") != -1) {
                outf.println("LARGE_PARTICLES");
                pol = this.overlay.get(this.overlay.getIndex("large particles")).getPolygon();
                for (i = 0; i < pol.npoints; i++) {
                    outf.println("  " + IJ.d2s(pol.xpoints[i], 0) + ", " + IJ.d2s(pol.ypoints[i], 0));
                }
                outf.println("END");
            }
            if (this.overlay.getIndex("random points") != -1) {
                outf.println("RANDOM_POINTS");
                pol = this.overlay.get(this.overlay.getIndex("random points")).getPolygon();
                for (i = 0; i < pol.npoints; i++) {
                    outf.println("  " + IJ.d2s(pol.xpoints[i], 0) + ", " + IJ.d2s(pol.ypoints[i], 0));
                }
                outf.println("END");
            }
            outf.close();
        } catch (Exception e) {
            return false;
        }
        writeIDtext(imp);
        drawComponents(imp);
        this.ntot++;
        SaveDialog sd = new SaveDialog("Save analyzed image",
                imp.getShortTitle(),
                ".a.tif");
        if (sd.getFileName() != null) {
            FileSaver saveTiff = new FileSaver(imp);
            saveTiff.saveAsTiff(sd.getDirectory() + sd.getFileName());
        }
        return true;
    }


    private void drawComponents(ImagePlus imp) {
        Polygon pol;
        int n, x, y;

        for (n = 0; n < this.overlay.size(); n++) {
            imp.setColor(this.overlay.get(n).getStrokeColor());
            if (this.overlay.get(n).getName().equals("small particles") ||
                this.overlay.get(n).getName().equals("large particles")) {
                pol = this.overlay.get(n).getPolygon();
                for (i = 0; i < pol.npoints; i++) {
                    x = pol.xpoints[i];
                    y = pol.ypoints[i];
                    imp.getProcessor().drawLine(x - 3, y, x + 3, y);
                    imp.getProcessor().drawLine(x, y - 3, x, y + 3);
                }
            }  else {
                this.overlay.get(n).drawPixels(imp.getProcessor());
            }
        }
    }


    private Point findxy(Polygon pol, ImagePlus imp) {
        int miny, x;

        miny = imp.getHeight();
        x = imp.getWidth();
        for (i = 0; i < pol.npoints; i++) {
            if (pol.ypoints[i] < miny) {
                miny = pol.ypoints[i];
                x = pol.xpoints[i];
                if (pol.ypoints[i] == miny && pol.xpoints[i] < x) {
                    x = pol.xpoints[i];
                }
            }
        }
        return new Point(x, miny);
    }


    private void writeIDtext(ImagePlus imp) {
        TextRoi profileLabel;
        Polygon pol;
        Point p;
        int locx, locy, size;
        String label = "";

        for (i = 0; i < this.ID.length(); i++) {
            label += this.ID.charAt(i);
        }
        size = imp.getHeight() / 42;  // adjust font size for image size (by an arbitrary factor)
        TextRoi.setFont(TextRoi.getFont(), size, Font.BOLD);
        profileLabel = new TextRoi(0, 0, label);
        profileLabel.setAntialiased(true);
        pol = this.overlay.get(this.overlay.getIndex("profile border")).getPolygon();
        p = findxy(pol, imp);
        locy = p.y - profileLabel.getBounds().height;
        locx = p.x - profileLabel.getBounds().width;
        if (locx < 0) locx = 3;
        if (locy < 0) locy = 3;
        profileLabel.setLocation(locx, locy);
        imp.setColor(textCol);
        profileLabel.drawPixels(imp.getProcessor());
        imp.setColor(Color.black);
    }


    public void clear() {
        this.dirty = false;
        this.overlay.clear();
        this.comment = "";
        this.ID = "";
    }


    /**
     * Main method for debugging.
     *
     * For debugging, it is convenient to have a method that starts ImageJ, loads an
     * image and calls the plugin, e.g. after setting breakpoints.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        // set the plugins.dir property to make the plugin appear in the Plugins menu
        Class<?> cl = ParticleDensityDouble_.class;
        String url = cl.getResource("/" + cl.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring(5, url.length() - cl.getName().length() - 6);
        System.setProperty("plugins.dir", pluginsDir);

        // start ImageJ
        new ImageJ();

        ImagePlus image = IJ.openImage("");
        image.show();

        // run the plugin
        IJ.runPlugIn(cl.getName(), "");
    }




} // end of ParticleDensityDouble
