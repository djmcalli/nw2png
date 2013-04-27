package org.graalcenter.nw2png;

import born2kill.nw2png.Listener;
import born2kill.nw2png.NW2PNGHelper;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

public class GraphicalNW2PNG implements Listener, ActionListener {
    NW2PNGHelper helper;
    private boolean isGenerating = false,filterOutput = true;
    
    File tilesetdir,sourcedir,outputdir,graaldir;
    
    JFrame frame;
    
    JLabel graaldirLabel;
    JLabel graaldirFileLabel;
    JButton graaldirBrowseButton;

    JLabel tilesetLabel;
    JLabel tilesetFileLabel;
    JButton tilesetBrowseButton;

    JLabel sourceLabel;
    JLabel sourceFileLabel;
    JButton sourceBrowseButton;

    JLabel outputLabel;
    JLabel outputFileLabel;
    JButton outputBrowseButton;

    JLabel scaleLabel;
    JComboBox scaleComboBox;
    
    JCheckBox filterToggle;

    JScrollPane logScrollPane;
    JTextPane logTextPane;

    JButton generateButton;

    JLabel creditLabel;
    
    public GraphicalNW2PNG() throws IOException {
        LoadDirs();
        helper = new NW2PNGHelper(this);

        String installDir = getInstallDir();

        if (installDir == null) {
            JOptionPane.showMessageDialog(null, "I was unable to find your Graal installation directory. You can still use me, but images won't work.");
        }

        //helper.setGraalDir(installDir);

        makeFrame();

        if (installDir != null) {
            // set the tileset to pics1.png
            if (tilesetdir == null) {
            File pics1 = new File(installDir + File.separator + "pics1.png");

            if (! pics1.exists()) { // try tiles folder
                pics1 = new File(installDir + File.separator + "levels" + File.separator + "tiles" + File.separator + "pics1.png");
            }

            if (pics1.exists()) {
                setTileset(pics1);
            }
            } else setTileset(tilesetdir);
            if (graaldir != null && graaldir.isDirectory()) setGraalDir(graaldir);
            if (sourcedir != null && sourcedir.isFile()) setSource(sourcedir);
            if (outputdir != null && outputdir.isFile()) setOutput(outputdir);
        }
    }

    private void setGraalDir(File file) {
        helper.setGraalDir(file.getAbsolutePath());
        graaldir = file;
        graaldirFileLabel.setText(file.getAbsolutePath());

        SaveDirs();
        updateGenerateButton();
    }

    private void setTileset(File file) {
        try {
            helper.setTileset(file);
            tilesetdir = file;
            tilesetFileLabel.setText(file.getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Can't load the image file. Sorry.");
        }

        SaveDirs();
        updateGenerateButton();
        
    }

    private void setSource(File file) {
        helper.setSourceFile(file);
        sourcedir = file;
        sourceFileLabel.setText(file.getName());

        SaveDirs();
        updateGenerateButton();
    }

    private void setOutput(File file) {
        outputdir = file;
        if (! file.getName().endsWith(".png")) {
            file = new File(file.getAbsoluteFile() + ".png");
        }
 
        helper.setOutputFile(file);
        outputFileLabel.setText(file.getName());

        SaveDirs();
        updateGenerateButton();
    }
    
    private void LoadDirs() {
      String fFileName = "saveddirs.txt";
      FileReader dirs_in;
      try {
        dirs_in = new FileReader(fFileName);
        BufferedReader dirs_reader = new BufferedReader(dirs_in);
        
        String dir_line = dirs_reader.readLine();
        
        String dirname;
       
        while (dir_line != null) {
          if (dir_line.startsWith("tilesetdir=")) {
            dirname = dir_line.substring(dir_line.indexOf("=") + 1);
            if (dir_line.indexOf("null") < 0) tilesetdir = new File(dirname);
          }
          
          if (dir_line.startsWith("sourcedir=")) {
            dirname = dir_line.substring(dir_line.indexOf("=") + 1);
            if (dir_line.indexOf("null") < 0) sourcedir = new File(dirname);
          }
          
          if (dir_line.startsWith("outputdir=")) {
            dirname = dir_line.substring(dir_line.indexOf("=") + 1);
            if (dir_line.indexOf("null") < 0) outputdir = new File(dirname);
          }
          
          if (dir_line.startsWith("graaldir=")) {
            dirname = dir_line.substring(dir_line.indexOf("=") + 1) + "\\";
            if (dir_line.indexOf("null") < 0) graaldir = new File(dirname);
          }
          
          dir_line = dirs_reader.readLine();
        }
        //if (sourcedir != null) setSource(sourcedir);
        //if (outputdir != null) setOutput(outputdir);
        
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    
    private void SaveDirs() {
      String fFileName = "saveddirs.txt";
      Writer out;
      try {
        out = new OutputStreamWriter(new FileOutputStream(fFileName));
        out.write("tilesetdir=" + tilesetdir + "\n");
        out.write("sourcedir=" + sourcedir + "\n");
        out.write("outputdir=" + outputdir + "\n");
        out.write("graaldir=" + graaldir + "\n");
        out.close();
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    private void updateGenerateButton() {
        generateButton.setEnabled(helper.getTileset() != null && helper.getSourceFile() != null && helper.getOutputFile() != null && ! isGenerating);
    }

    private void makeFrame() {
        frame = new JFrame("NW2PNG");
        frame.setMinimumSize(new Dimension(400, 400));
        frame.setPreferredSize(new Dimension(400, 400));
        frame.setLocationRelativeTo(null);
        frame.setLayout(new GridBagLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        GridBagConstraints constraints;
        Container pane = frame.getContentPane();
        JPanel topPanel = new JPanel(new GridBagLayout());

        constraints = new GridBagConstraints();
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        int padding = 10;
        constraints.insets = new Insets(padding, padding, padding, padding);

        pane.add(topPanel, constraints);
        
        // Graal Directory
        graaldirLabel = new JLabel("Graal Dir:");
        graaldirLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        constraints = new GridBagConstraints();
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(0, 0, 1, 5);

        topPanel.add(graaldirLabel, constraints);

        graaldirFileLabel = new JLabel("<none>");
        graaldirFileLabel.setHorizontalAlignment(SwingConstants.LEFT);

        constraints = new GridBagConstraints();
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.insets = new Insets(0, 0, 1, 0);

        topPanel.add(graaldirFileLabel, constraints);

        graaldirBrowseButton = new JButton("Browse");
        graaldirBrowseButton.addActionListener(this);

        constraints = new GridBagConstraints();
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.insets = new Insets(0, 0, 1, 0);

        topPanel.add(graaldirBrowseButton, constraints);

        // Tileset
        tilesetLabel = new JLabel("Tileset:");
        tilesetLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        constraints = new GridBagConstraints();
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets = new Insets(0, 0, 1, 5);

        topPanel.add(tilesetLabel, constraints);

        tilesetFileLabel = new JLabel("<none>");
        tilesetFileLabel.setHorizontalAlignment(SwingConstants.LEFT);

        constraints = new GridBagConstraints();
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.insets = new Insets(0, 0, 1, 0);

        topPanel.add(tilesetFileLabel, constraints);

        tilesetBrowseButton = new JButton("Browse");
        tilesetBrowseButton.addActionListener(this);

        constraints = new GridBagConstraints();
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 2;
        constraints.gridy = 1;
        constraints.insets = new Insets(0, 0, 1, 0);

        topPanel.add(tilesetBrowseButton, constraints);

        // Source
        sourceLabel = new JLabel("Source:");
        sourceLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        constraints = new GridBagConstraints();
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.insets = new Insets(0, 0, 1, 5);

        topPanel.add(sourceLabel, constraints);

        sourceFileLabel = new JLabel("<none>");
        sourceFileLabel.setHorizontalAlignment(SwingConstants.LEFT);

        constraints = new GridBagConstraints();
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.insets = new Insets(0, 0, 1, 0);

        topPanel.add(sourceFileLabel, constraints);

        sourceBrowseButton = new JButton("Browse");
        sourceBrowseButton.addActionListener(this);

        constraints = new GridBagConstraints();
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 2;
        constraints.gridy = 2;
        constraints.insets = new Insets(0, 0, 1, 0);

        topPanel.add(sourceBrowseButton, constraints);

        // Output
        outputLabel = new JLabel("Output:");
        outputLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        constraints = new GridBagConstraints();
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.insets = new Insets(0, 0, 5, 5);

        topPanel.add(outputLabel, constraints);

        outputFileLabel = new JLabel("<none>");
        outputFileLabel.setHorizontalAlignment(SwingConstants.LEFT);

        constraints = new GridBagConstraints();
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.insets = new Insets(0, 0, 5, 0);

        topPanel.add(outputFileLabel, constraints);

        outputBrowseButton = new JButton("Browse");
        outputBrowseButton.addActionListener(this);

        constraints = new GridBagConstraints();
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 2;
        constraints.gridy = 3;
        constraints.insets = new Insets(0, 0, 5, 0);

        topPanel.add(outputBrowseButton, constraints);

        // Scale
        scaleLabel = new JLabel("Scale:");
        scaleLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        constraints = new GridBagConstraints();
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.insets = new Insets(0, 0, 5, 5);

        topPanel.add(scaleLabel, constraints);

        String[] options = {"1:1", "1:2", "1:4", "1:8", "1:16", "1:32", "1:64"};
        scaleComboBox = new JComboBox(options);

        constraints = new GridBagConstraints();
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 1;
        constraints.gridy = 4;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(0, 0, 5, 0);

        topPanel.add(scaleComboBox, constraints);

        // log
        logTextPane = new JTextPane();
        logTextPane.setEditable(false);
        logScrollPane = new JScrollPane(logTextPane);
        logScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        constraints = new GridBagConstraints();
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridwidth = 3;
        constraints.insets = new Insets(0, 0, 5, 0);

        topPanel.add(logScrollPane, constraints);

        //    JCheckBox filterToggle;
        /*
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.insets = new Insets(0, 0, 5, 5);
        */
        // Toggle filter
        filterToggle = new JCheckBox("Filter Output");
        filterToggle.addActionListener(this);

        constraints = new GridBagConstraints();
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = 0;
        constraints.insets = new Insets(0, 0, 5, 0);

        topPanel.add(filterToggle, constraints);

        
        // Generate
        generateButton = new JButton("Generate");
        generateButton.addActionListener(this);

        constraints = new GridBagConstraints();
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = 3;
        constraints.insets = new Insets(0, 0, 5, 0);

        topPanel.add(generateButton, constraints);
        
        // credit
        creditLabel = new JLabel("<html><p align=\"left\">Originally written by Alex (born2kill).<br>Modifications by Dusty and Chris Vimes.GUI by Chris Vimes.</p></html>");

        constraints = new GridBagConstraints();
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.gridx = 0;
        constraints.gridy = 7;
        constraints.gridwidth = 3;

        topPanel.add(creditLabel, constraints);
        
        // update generate button enabled status
        updateGenerateButton();

        // show frame
        frame.pack();
        frame.setVisible(true);
        
        filterToggle.setSelected(helper.getFilter());
    }

    public static void main(String[] args) throws IOException {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
        }
        
        new GraphicalNW2PNG();
    }

    private static String getInstallDir() {
        String userDir = System.getProperty("user.home");
        String[] tryDirs = {userDir + File.separator + ".graal" + File.separator + "graal4", userDir + File.separator + "Library" + File.separator + "Application Support" + File.separator + "Graal", File.separator + "Graal", File.separator + "Program Files" + File.separator + "Graal", File.separator + "Program Files (x86)" + File.separator + "Graal", userDir + File.separator + "Graal", userDir + File.separator + "Documents" + File.separator + "Graal", userDir + File.separator + "My Saved Games" + File.separator + "Graal", userDir + File.separator + "Desktop" + File.separator + "Graal"};

        for (String tryDir : tryDirs) {
            File options = new File(tryDir + File.separator + "game_config.txt");

            if (options.exists()) {
                return tryDir;
            }
        }

        return null;
    }

    public void sendMessage(String message) {
        String timeStamp = getTimeStamp();

        if (logTextPane.getText().length() > 0) {
            logTextPane.setText(logTextPane.getText() + "\n");
        }

        logTextPane.setText(logTextPane.getText() + "[" + timeStamp + "]: " + message);
        logTextPane.setCaretPosition(logTextPane.getDocument().getLength());
    }

    public String getTimeStamp() {
        Date myDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(myDate);
    }

    public void actionPerformed(ActionEvent e) {
        Component object = (Component) e.getSource();

        if (object == graaldirBrowseButton) {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setCurrentDirectory(graaldir);
            int returnVal = fc.showOpenDialog(object);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                setGraalDir(fc.getSelectedFile());
            }
        } if (object == tilesetBrowseButton) {
            JFileChooser fc = new JFileChooser();
            fc.setCurrentDirectory(tilesetdir);
            int returnVal = fc.showOpenDialog(object);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                setTileset(fc.getSelectedFile());
            }
        } else if (object == sourceBrowseButton) {
            JFileChooser fc = new JFileChooser();
            fc.setCurrentDirectory(sourcedir);
            int returnVal = fc.showOpenDialog(object);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                setSource(fc.getSelectedFile());
            }
        } else if (object == outputBrowseButton) {
            JFileChooser fc = new JFileChooser();
            fc.setCurrentDirectory(outputdir);
            int returnVal = fc.showSaveDialog(object);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                setOutput(fc.getSelectedFile());
            }
        } else if (object == filterToggle) {
          filterOutput = filterToggle.isSelected();
          helper.setFilter(filterOutput);
        } else if (object == generateButton) {
            double scale = (1 / Math.pow(2, scaleComboBox.getSelectedIndex()));
            helper.setScale(scale);
            isGenerating = true;
            updateGenerateButton();

            logTextPane.setText("");
            sendMessage("Generating...");

            helper.generate();
        }
    }

    public void doneGenerating() {
        isGenerating = false;
        updateGenerateButton();
    }
}