/*
NW2PNG by Alex (born2kill)
http://forums.graalonline.com/forums/showthread.php?t=134259601

Modifications by Chris Vimes
 */
package born2kill.nw2png;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.ImageIcon;

import java.util.regex.Pattern;

public class NW2PNGHelper implements Runnable {
    private BufferedImage tileset = null;
    private double scale = 1;
    private File sourceFile;
    private File outputFile;
    private Listener listener;
    private String graalDir = "C:\\Program Files\\Graal\\",filenamecacheDir;
    private boolean renderinggmap = false,filterOutput = true;
    
    int ganiOffsetx = 0;
    int ganiOffsety = 0;
    
    ArrayList<String[]> tiledefs = new ArrayList<String[]>();
    
    public Listener getListener() {
        return listener;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }
    
    public boolean getFilter() {
      return filterOutput;
  }
    
    public void setFilter(boolean filter) {
      this.filterOutput = filter;
  }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public String getGraalDir() {
        return graalDir;
    }

    public void setGraalDir(String graalDir) {
        this.graalDir = graalDir;
    }

    public BufferedImage getTileset() {
        return tileset;
    }

    public NW2PNGHelper(Listener listener) {
        setListener(listener);
    }

    public void setTileset(File tilesetFile) throws IOException {
        tileset = ImageIO.read(tilesetFile);
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public void generate() {
        Thread runner = new Thread(this);
        runner.start();
    }
    
    public void run() {
      String source_path = getSourceFile().getAbsolutePath();
      tiledefs = new ArrayList<String[]>();
      
      CheckFILENAMECACHE();
      
      Date time = new Date();
      long startTime = time.getTime();
      
      try {
        
        if (source_path.endsWith(".nw")) {
          BufferedImage renderNW = renderLevel(getSourceFile());
          if (renderinggmap == false) getListener().sendMessage("Saving image...");
            try {
              File file = getOutputFile();
              ImageIO.write(renderNW, "png", file);
              if (renderinggmap == false) {
                getListener().sendMessage("The image has been saved successfully!");
                time = new Date();
                getListener().sendMessage("Parsed and rendered in " + (int)((time.getTime() - startTime)/1000) + " seconds.");
              }
          } catch (IOException e) {
            getListener().sendMessage("Error: Couldn't save the image");
          }
        } else if (source_path.endsWith(".gmap")) {
          BufferedImage renderGmap = renderGmap(getSourceFile());
          getListener().sendMessage("Saving image...");
          try {
            File file = getOutputFile();
            ImageIO.write(renderGmap, "png", file);
            getListener().sendMessage("The image has been saved successfully!");
            time = new Date();
            getListener().sendMessage("Parsed and rendered in " + (int)((time.getTime() - startTime)/1000) + " seconds.");
          } catch (IOException e) {
            renderinggmap = false;
            getListener().sendMessage("Error: Couldn't save the image");
          }
        }
      } catch (OutOfMemoryError e) {
        renderinggmap = false;
        getListener().sendMessage("Error: Out of memory! Try MoreMemory.bat");
        getListener().doneGenerating();
      }
      renderinggmap = false;
      getListener().doneGenerating();
    }

    private BufferedImage renderLevel(File source) {
        String level_name = source.getName();
        String sourcePath = source.getAbsolutePath();
        int intDimension = (int) (1024 * getScale());
        int intTile = (int) (16 * getScale());

        try {
            FileReader level_in = new FileReader(source);
            BufferedReader level_reader = new BufferedReader(level_in);

            BufferedImage gmap_tiles = new BufferedImage(intDimension, intDimension, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D tiles_g2d = gmap_tiles.createGraphics();
            if (getFilter()) tiles_g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            else tiles_g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            BufferedImage gmap_npcs = new BufferedImage(intDimension, intDimension, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D npcs_g2d = gmap_npcs.createGraphics();
            if (getFilter()) npcs_g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            else npcs_g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

            ArrayList<String[][]> ganis = new ArrayList<String[][]>();
            ArrayList<String> level_file = new ArrayList<String>();
            int level_highest_layer = 0;
           
            String level_file_line = level_reader.readLine();
            
            while (level_file_line != null) {
                if (level_file_line.startsWith("BOARD ")) {
                    level_file.add(level_file_line);
                    level_file_line = level_file_line.substring(6);
                    level_file_line = level_file_line.substring(level_file_line.indexOf(' ') + 1);
                    level_file_line = level_file_line.substring(level_file_line.indexOf(' ') + 1);
                    level_file_line = level_file_line.substring(level_file_line.indexOf(' ') + 1);

                    int level_current_layer = Integer.parseInt(level_file_line.substring(0, level_file_line.indexOf(' ')));
                    if (level_current_layer > level_highest_layer) {
                        level_highest_layer = level_current_layer;
                    }
                } else if (level_file_line.indexOf("addtiledef2") >= 0 ) {
                    level_file_line = level_file_line.substring(level_file_line.indexOf("addtiledef2") + 12);
                    
                    level_file_line = level_file_line.replace("(","");
                    level_file_line = level_file_line.replace(")","");
                    level_file_line = level_file_line.replaceAll("\"","");
                    level_file_line = level_file_line.replace("#L",level_name);
                    
                    final String REGEX = ",";
                    Pattern p = Pattern.compile(REGEX);
                    String[] partitems = p.split(level_file_line);
                    
                    String imgname    = partitems[0];
                    String definition = partitems[1];
                    String drawx      = partitems[2];
                    String drawy      = partitems[3].substring(0,partitems[3].indexOf(";"));
                  
                    String[] temparr = {definition,imgname,drawx,drawy};
                    tiledefs.add(temparr);
                } else if (level_file_line.indexOf("addtiledef") >= 0 && level_file_line.indexOf("addtiledef2") < 0) {
                  level_file_line = level_file_line.substring(level_file_line.indexOf("addtiledef") + 11);
                  
                  level_file_line = level_file_line.replace("(","");
                  level_file_line = level_file_line.replace(")","");
                  level_file_line = level_file_line.replaceAll("\"","");
                  level_file_line = level_file_line.replace("#L",level_name);
                  
                  final String REGEX = ",";
                  Pattern p = Pattern.compile(REGEX);
                  String[] partitems = p.split(level_file_line);
                  
                  String imgname    = partitems[0];
                  String definition = partitems[1];
                  String[] temparr = {definition,imgname,Integer.toString(0),Integer.toString(0)};
                  tiledefs.add(temparr);
              }  else if (level_file_line.startsWith("NPC ")) {
                  double NPCx = 0;
                  double NPCy = 0;
                  String[] parsexy = level_file_line.split("\\s+");
                  NPCx = Integer.parseInt(parsexy[2]);
                  NPCy = Integer.parseInt(parsexy[3]);
                  
                  level_reader.mark(300);
                  
                  // attrs = {gani,#c0,#c1,#c2,#c3,#c4,#P1,#P2,#P3,#1,#2,#3,#8,param1,dir};
                  String[] attrs = new String[15];
                  boolean foundshowcharacter = false;

                  String npc_imgpart = level_reader.readLine();
                  boolean foundgani = false;
                  boolean foundsetimgpart = false;
                  for (int j = 0; j < 300; j++) {
                    npc_imgpart = npc_imgpart.trim();
                    if (npc_imgpart.indexOf("setcharprop") > -1) {
                      String prop = npc_imgpart.substring(12,npc_imgpart.indexOf(",")).toLowerCase();
                      String propvalue = npc_imgpart.substring(npc_imgpart.indexOf(",")+1,npc_imgpart.indexOf(";"));
                      if (prop.equals("#c0")) attrs[1] = propvalue;
                      else if (prop.equals("#c1")) attrs[2] = propvalue;
                      else if (prop.equals("#c2")) attrs[3] = propvalue;
                      else if (prop.equals("#c3")) attrs[4] = propvalue;
                      else if (prop.equals("#c4")) attrs[5] = propvalue;
                      else if (prop.equals("#p1")) attrs[6] = propvalue;
                      else if (prop.equals("#p2")) attrs[7] = propvalue;
                      else if (prop.equals("#p3")) attrs[8] = propvalue;
                      else if (prop.equals("#1"))  attrs[9] = propvalue;
                      else if (prop.equals("#2"))  attrs[10] = propvalue;
                      else if (prop.equals("#3"))  attrs[11] = propvalue;
                      else if (prop.equals("8"))   attrs[12] = propvalue;
                    } else if (npc_imgpart.indexOf("dir") > -1) {
                      npc_imgpart = npc_imgpart.replaceAll("\\s+","");
                      npc_imgpart = npc_imgpart.replace("this.","");
                      int findequal = npc_imgpart.indexOf("=")+1;
                      
                      String test_dir = npc_imgpart.substring(findequal,findequal+1);
                      try {
                        Integer.parseInt(test_dir);
                        attrs[14] = npc_imgpart.substring(findequal,findequal+1);
                      } catch(Exception e) {
                      }
                    } else if (npc_imgpart.indexOf("showcharacter") > -1) {
                      foundshowcharacter = true;
                    } else if (npc_imgpart.trim().startsWith("x") || npc_imgpart.trim().startsWith("this.x")) {
                      npc_imgpart = npc_imgpart.trim();
                      npc_imgpart = npc_imgpart.replace("this.","");
                      npc_imgpart = npc_imgpart.replaceAll("\\s+","");
                      npc_imgpart = npc_imgpart.replaceAll("\\+","&#43;");
                      if (npc_imgpart.indexOf("&#43;&#43;") > -1) NPCx += 1;
                      else if (npc_imgpart.indexOf("--") > -1) NPCx -= 1;
                      else if (npc_imgpart.indexOf("&#43;=") > -1) {
                        String[] npc_tokens = npc_imgpart.split("&#43;=");
                        NPCx += findDouble(npc_tokens[1]);
                      } else if (npc_imgpart.indexOf("-=") > -1) {
                        String[] npc_tokens = npc_imgpart.split("-=");
                        NPCx -= findDouble(npc_tokens[1]);
                      } else if (npc_imgpart.indexOf("=") > -1) {
                        String[] npc_tokens = npc_imgpart.split("=");
                        NPCx = findDouble(npc_tokens[1]);
                      }
                    } else if (npc_imgpart.trim().startsWith("y") || npc_imgpart.trim().startsWith("this.y")) {
                      npc_imgpart = npc_imgpart.trim();
                      npc_imgpart = npc_imgpart.replace("this.","");
                      npc_imgpart = npc_imgpart.replaceAll("\\s+","");
                      npc_imgpart = npc_imgpart.replaceAll("\\+","&#43;");
                      if (npc_imgpart.indexOf("&#43;&#43;") > -1) NPCy += 1;
                      else if (npc_imgpart.indexOf("--") > -1) NPCy -= 1;
                      else if (npc_imgpart.indexOf("&#43;=") > -1) {
                        String[] npc_tokens = npc_imgpart.split("&#43;=");
                        NPCy += findDouble(npc_tokens[1]);
                      } else if (npc_imgpart.indexOf("-=") > -1) {
                        String[] npc_tokens = npc_imgpart.split("-=");
                        NPCy -= findDouble(npc_tokens[1]);
                      } else if (npc_imgpart.indexOf("=") > -1) {
                        String[] npc_tokens = npc_imgpart.split("=");
                        NPCy = findDouble(npc_tokens[1]);
                      }
                    }  else if (npc_imgpart.indexOf("setcharani") > -1 && npc_imgpart.indexOf("else") < 0 && foundgani == false) {
                        level_file_line = level_file_line.replace("this.","");
                        level_file_line = level_file_line.replace("(","");
                        level_file_line = level_file_line.replace(")","");
                        level_file_line = level_file_line.replaceAll("\"","");

                        npc_imgpart = npc_imgpart.substring(npc_imgpart.indexOf("setcharani")+11).toLowerCase();
                        String ganiname = npc_imgpart.substring(0,npc_imgpart.indexOf(","));
                        String ganiparam = npc_imgpart.substring(npc_imgpart.indexOf(",")+1,npc_imgpart.indexOf(";"));
                      
                        attrs[0] = ganiname + ".gani";
                        attrs[13] = ganiparam;
                        
                        foundgani = true;
                    } else if (npc_imgpart.indexOf("setimgpart") > -1) {
                      npc_imgpart = npc_imgpart.replaceAll("\"","");
                      npc_imgpart = npc_imgpart.replace("this.","");
                      npc_imgpart = npc_imgpart.replace("("," ");
                      npc_imgpart = npc_imgpart.replace(")","");
                      
                      int startparse = npc_imgpart.indexOf(",");
                      int endparse;
                      if (npc_imgpart.indexOf(")") > -1) endparse = npc_imgpart.indexOf(")");
                      else endparse = npc_imgpart.indexOf(";");
                      String partdata = npc_imgpart.substring(startparse,endparse);
                      partdata = partdata.replaceAll("\\s+","");

                      String imgpartvalues = partdata.replace(","," ");
                      level_file.add(level_file_line + imgpartvalues);
                      //level_file.add("NPCPART " + partdata.replace(","," "));
                      foundsetimgpart = true;
                      break;
                    } else if (npc_imgpart.startsWith("NPCEND")) {
                      if (foundshowcharacter == true && attrs[0] == null) attrs[0] = "idle.gani";
                      if (attrs[0] != null) {
                        String[] pos = {String.valueOf(NPCx),String.valueOf(NPCy)};
                        String[][] concat = {attrs,pos};
                        ganis.add(concat);
                      }
                      break;
                    }
                    
                    npc_imgpart = level_reader.readLine();
                  }
                  if (foundsetimgpart == false && !level_file_line.startsWith("NPC -")) level_file.add(level_file_line + " 0 0 -1 -1");
                  level_reader.reset();
                }

                level_file_line = level_reader.readLine();
            }

            level_in.close();

            int[][][] level_tiles = new int[64][64][level_highest_layer + 1];
            for (int level_y = 0; level_y < level_tiles.length; level_y ++) {
                for (int level_x = 0; level_x < level_tiles[level_y].length; level_x ++) {
                    for (int level_layer = 1; level_layer < level_tiles[level_y][level_x].length; level_layer ++) {
                        level_tiles[level_y][level_x][level_layer] = -1;
                    }
                }
            }

            ArrayList<String[]> level_npcs = new ArrayList<String[]>();

            for (String level_line : level_file) {
                String line = level_line;

                if (line.startsWith("BOARD ")) {
                    line = line.substring(6);
                    int tiles_start = Integer.parseInt(line.substring(0, line.indexOf(' ')));

                    line = line.substring(line.indexOf(' ') + 1);
                    int tiles_height = Integer.parseInt(line.substring(0, line.indexOf(' ')));
                    if (tiles_height > 63) {
                        tiles_height = 63;
                    }

                    line = line.substring(line.indexOf(' ') + 1);
                    int tiles_width = Integer.parseInt(line.substring(0, line.indexOf(' ')));
                    if (tiles_width > 64) {
                        tiles_width = 64;
                    }

                    line = line.substring(line.indexOf(' ') + 1);
                    int tiles_layer = Integer.parseInt(line.substring(0, line.indexOf(' ')));

                    line = line.substring(line.indexOf(' ') + 1);
                    for (int level_x = tiles_start; level_x < tiles_width; level_x ++) {
                        level_tiles[tiles_height][level_x][tiles_layer] = getTileNumber(line.substring(level_x * 2, level_x * 2 + 2));
                    }
                } else if (line.startsWith("NPC ")) {
                  line = line.substring(4);

                  final String REGEX = " ";
                  Pattern p = Pattern.compile(REGEX);
                  String[] partitems = p.split(line);
                 
                  String image_name;
                  
                  int image_x,image_y,image_dx,image_dy,image_dw,image_dh;

                  image_name = partitems[0];
                  image_x     = findInt(partitems[1]);
                  image_y     = findInt(partitems[2]);
                  image_dx    = findInt(partitems[3]);
                  image_dy    = findInt(partitems[4]);
                  image_dw    = findInt(partitems[5]);
                  image_dh    = findInt(partitems[6]);
                  
                  if (image_x < -64 || image_x > 127) {
                    getListener().sendMessage("Warning : The images x of " + image_name + " is " + (image_x < -64 ? "smaller then -64" : "bigger then 127"));
                    continue;
                  }

                  if (image_y < -64 || image_y > 127) {
                    getListener().sendMessage("Warning : The images y of " + image_name + " is " + (image_y < -64 ? "smaller then -64" : "bigger then 127"));
                    continue;
                  }
                  String[] temp_arr = {image_name,Integer.toString(image_x),Integer.toString(image_y),Integer.toString(image_dx),Integer.toString(image_dy),Integer.toString(image_dw),Integer.toString(image_dh)};
                  level_npcs.add(temp_arr);
               }
            }

            BufferedImage finaltileset = new BufferedImage(tileset.getWidth(),tileset.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics g = finaltileset.getGraphics();
            g.drawImage(tileset, 0, 0, null);
            
            for (String[] i : tiledefs) {
              if (level_name.startsWith(i[0])) {
                try {
                  BufferedImage tiledef_image = ImageIO.read(new File(getImageLocation(i[1])));
                  if (tiledef_image != null) {
                    g.drawImage(tiledef_image,findInt(i[2]),findInt(i[3]), null);
                  }
                } catch (IOException e) {
                  getListener().sendMessage("Error: Couldn't load the file " + i[1]);
                }
              }
            }
            
            for (int level_y = 0; level_y < level_tiles.length; level_y ++) {
                for (int level_x = 0; level_x < level_tiles[level_y].length; level_x ++) {
                    for (int level_layer = 0; level_layer < level_tiles[level_y][level_x].length; level_layer ++) {
                        if (level_tiles[level_y][level_x][level_layer] < 0) {
                            continue;
                        }
                        int[] tile_xy = getTileXY(level_tiles[level_y][level_x][level_layer]);
                        tiles_g2d.drawImage(finaltileset, level_x * intTile, level_y * intTile, level_x * intTile + intTile, level_y * intTile + intTile, tile_xy[0], tile_xy[1], tile_xy[0] + 16, tile_xy[1] + 16, null);
                    }
                }
            }
            
            for (String[][] gani: ganis) {
              BufferedImage gani_render = getGani(gani[0]);
              if (gani_render == null) {
                getListener().sendMessage("Warning : Couldn't render the gani " + gani[0][0]);
                continue;
              }
              //System.out.println(gani[0][0] + " : " + gani[1][0] + "," + gani[1][1]);
              int NPCx = (int)(Double.parseDouble(gani[1][0]) * intTile + (int)(ganiOffsetx*getScale()));
              int NPCy = (int)(Double.parseDouble(gani[1][1]) * intTile + (int)(ganiOffsety*getScale()));
              int NPCw = (int)(gani_render.getWidth()*getScale());
              int NPCh = (int)(gani_render.getHeight()*getScale());
              
              //System.out.println("Rendering: " + gani[0][0] + " : " + ganiOffsetx + " : " + ganiOffsety);
              npcs_g2d.drawImage(gani_render,NPCx,NPCy,NPCw,NPCh,null);
 
            }

            for (String[] npc : level_npcs) {
              if (npc[0] == null || npc[0].toLowerCase() == "pics1.png" || npc[0].toLowerCase().contains("light")) continue;
              int image_x  = findInt(npc[1]);
              int image_y  = findInt(npc[2]);
              int image_dx = findInt(npc[3]);
              int image_dy = findInt(npc[4]);
              int image_dw = findInt(npc[5]);
              int image_dh = findInt(npc[6]);
              
              try {
                BufferedImage npc_image = ImageIO.read(new File(getImageLocation(npc[0])));
                if (npc_image == null) {
                    getListener().sendMessage("Warning : Unknown image type " + npc[0].substring(npc[0].lastIndexOf(".") + 1).toUpperCase());
                } else {
                    image_dw = image_dw == -1 ? npc_image.getWidth() : image_dw;
                    image_dh = image_dh == -1 ? npc_image.getHeight() : image_dh;
                    
                    int render_x = (image_x) * intTile;
                    int render_y = (image_y) * intTile;
                    int render_w = (int)(image_dw * getScale());
                    int render_h = (int)(image_dh * getScale());

                    npcs_g2d.drawImage(npc_image,render_x,render_y,render_x + render_w,render_y + render_h,
                                       image_dx,image_dy,image_dx+image_dw,image_dy+image_dh,null);
                 }
              } catch (IOException e) {
                getListener().sendMessage("Warning : Couldn't find the image " + npc[0]);
              }
            }

            if (renderinggmap == false) getListener().sendMessage("Rendering and saving the image...");

            npcs_g2d.dispose();
            tiles_g2d.drawImage(gmap_npcs, 0, 0, null);
            tiles_g2d.dispose();
            
            if (gmap_tiles != null) return gmap_tiles;

        } catch (IOException e) {
          getListener().sendMessage("Error: Couldn't load the file " + sourcePath.substring(sourcePath.lastIndexOf(File.separator) + 1));
        }
        getListener().sendMessage("Error: Level was unable to be rendered for an unknown reason.");
        return null;
    }

    private BufferedImage renderGmap(File source) {
      renderinggmap = true;
      String sourcePath = source.getAbsolutePath();
      int intTile = (int) (16 * getScale());

      try {
        FileReader gmap_in;
        gmap_in = new FileReader(source);
        BufferedReader gmap_reader = new BufferedReader(gmap_in);

        BufferedImage gmap_tiles,gmap_npcs;
        Graphics2D tiles_g2d,npcs_g2d;

        int gmap_width = 0,gmap_height = 0;

        boolean parselevels = false;
        int gmap_y = 0;
        
        String[] levels = new String[0];
        
        String gmap_line = gmap_reader.readLine();
        int gmap_yrender = 0;
        while (gmap_line != null) {
          if (gmap_width < 0 || gmap_height < 0) break;
          
          if (gmap_line.startsWith("LEVELNAMESEND")) {
            parselevels = false;
            gmap_line = gmap_reader.readLine();
            continue;
          } 
 
          if (gmap_line.startsWith("WIDTH ")) {
            gmap_width = Integer.parseInt(gmap_line.substring(6));
            levels = new String[gmap_width*gmap_height];
          } else if (gmap_line.startsWith("HEIGHT ")) {
            gmap_height = Integer.parseInt(gmap_line.substring(7));
            levels = new String[gmap_width*gmap_height];
          } else if (gmap_line.startsWith("LEVELNAMES")) {
            parselevels = true;
          } else if (parselevels == true) {
            String[] level_tokens = gmap_line.split(",");
           
            
            if (level_tokens.length > gmap_width) {
              getListener().sendMessage("Error: GMAP format is incorrent!");
              return null;
            }
            
            for (int i=0;i<level_tokens.length;i++) {
              String levelname = level_tokens[i].replaceAll("\"","");
              levelname = levelname.trim();
              if (i + gmap_yrender*gmap_width < levels.length) levels[i + gmap_yrender*gmap_width] = levelname;
              else {
                System.out.println(i + "," + gmap_yrender + "," + gmap_width + " : " + levels.length);
                getListener().sendMessage("Error: Trouble parsing GMAP level data!");
              }
            }
            
            gmap_yrender++;
          }
          gmap_line = gmap_reader.readLine();
        }
        
        int gmap_image_width = (int) (gmap_width * 64 * 16 * getScale());
        int gmap_image_height = (int) (gmap_height * 64 * 16 * getScale());
        
        BufferedImage gmapImage = new BufferedImage(gmap_image_width,gmap_image_height, BufferedImage.TYPE_INT_ARGB);
        Graphics g = gmapImage.getGraphics();
        
        g.setColor(new Color(0));
        g.fillRect(0,0,gmapImage.getWidth(),gmapImage.getHeight());
        for (int i = 0;i < levels.length;i++) {
          File nwFile = new File(sourcePath.substring(0, sourcePath.lastIndexOf(File.separator) + 1) + levels[i]);
    
          BufferedImage nw_render = renderLevel(nwFile);
          
          if (nw_render == null) continue;
          
          getListener().sendMessage("Rendering level: " + nwFile.getName());
         
          int draw_x = (i%gmap_width) * nw_render.getWidth();
          int draw_y = (int)(i/gmap_width) * nw_render.getHeight();
          
          g.drawImage(nw_render,draw_x,draw_y,null);
          //g.drawImage(nw_render,draw_x,draw_y,draw_x + nw_render.getWidth(),draw_y + nw_render.getHeight(),null);
        }
        
        if (gmapImage != null) return gmapImage;
        
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
      getListener().sendMessage("Error: GMAP was unable to be rendered for an unknown reason.");
      return null;
    }

    private int getTileNumber(String tile_string) {
        String base64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        return base64.indexOf(tile_string.substring(0, 1)) * 64 + base64.indexOf(tile_string.substring(1, 2));
    }

    private int[] getTileXY(int tile_number) {
        int[] tile_xy = {(tile_number % 16 + tile_number / 512 * 16) * 16, (tile_number / 16 % 32) * 16};
        return tile_xy;
    }
    
    private void CheckFILENAMECACHE() {
      filenamecacheDir = getGraalDir();
      if (!filenamecacheDir.endsWith("\\")) filenamecacheDir = filenamecacheDir + "\\";

      File filecheck = new File(filenamecacheDir + "FILENAMECACHE.txt"),filecheck2;
      if (filecheck.exists() == false) {
        getListener().sendMessage("Error: Failed to find FILENAMECACHE.txt in " + filenamecacheDir + ", falling back on C:\\Program Files\\Graal\\...");
        filenamecacheDir = "C:\\Program Files\\Graal\\";
        filecheck2 = new File(filenamecacheDir + "FILENAMECACHE.txt");
        if (filecheck2.exists() == false) getListener().sendMessage("Error: Failed to find FILENAMECACHE.txt on fallback search.");
      } else {
        
      }
    }

    private String getImageLocation(String imageName) {
      String GraalFolder = filenamecacheDir;
      
        try {
          //FileReader filenamecache = new FileReader(getGraalDir().substring(0, getGraalDir().lastIndexOf(File.separator) + 1) + "FILENAMECACHE.txt");
          
          FileReader filenamecache = new FileReader(GraalFolder + "FILENAMECACHE.txt");
     
          BufferedReader cache_scan = new BufferedReader(filenamecache);
          String cache_scan_line = cache_scan.readLine();
          
          while (cache_scan_line != null) {
            if (cache_scan_line.indexOf("\\" + imageName) > -1 || cache_scan_line.startsWith(imageName)) {
              return GraalFolder + cache_scan_line.substring(0,cache_scan_line.indexOf(","));
            }
            cache_scan_line = cache_scan.readLine();
          }
          
        } catch (FileNotFoundException e) {
          //getListener().sendMessage("Error: Failed to find " + imageName);
        } catch (IOException e) {
          //getListener().sendMessage("Error: Failed to load " + imageName);
        }
        return GraalFolder;
    }
    
    private BufferedImage getGani(String[] attr) {
      // attrs = {gani,#c0,#c1,#c2,#c3,#c4,#P1,#P2,#P3,#1,#2,#3,#8,param1,dir};
      //if (tiledef_image != null) {
      //  g.drawImage(tiledef_image,Integer.parseInt(i[2]),Integer.parseInt(i[3]), null);
      
      String ganiName = attr[0];
      if (ganiName == null) return null;
      int dir = 2;
      if (attr[14] != null) {
        dir = Integer.parseInt(attr[14]);
      }
      
      ganiOffsetx = ganiOffsety = 0;
      
      // DEFINE DEFAULTS
      String[] defaultcolors = {"white" ,"yellow","orange","pink"  ,"red"   ,"darkred","lightgreen","green" ,"darkgreen","lightblue","blue"  ,"darkblue","brown" ,"cynober","purple","darkpurple","lightgray","gray"  ,"black" };               
      int[] defaultcolorshex = {0xffffff,0xffff00,0xffad6b,0xff8484,0xff0000,0xce1829 ,0x84ff84    ,0x00ff00,0x00c600   ,0x8484ff   ,0x0000ff,0x0000c6  ,0x840000,0x00ffff ,0xff00ff,0x840084    ,0xcecece   ,0x848484,0x000008};
      
      String img_sprites   = "sprites.png";
      String img_attr1     = null;
      String img_attr2     = null;
      String img_attr3     = null;
      String img_sword     = "sword1.png";
      String img_shield    = "shield1.png";
      String img_head      = "head19.png";
      String img_body      = "body.png";
      String img_param1    = null;
      
      String color_skin    = "orange";
      String color_coat    = "white";
      String color_sleeves = "red";
      String color_shoes   = "blue";
      String color_belt    = "black";
      
      String[][] sprites;
      ArrayList<int[]> rendersprites = new ArrayList<int[]>();
      
      FileReader gani_in;
      
      try {
        gani_in = new FileReader(getImageLocation(ganiName));
        BufferedReader gani_reader = new BufferedReader(gani_in);
        
        gani_reader.mark(102400);
        
        String gani_line = gani_reader.readLine();
        String sprite_scan = gani_reader.readLine();
        
        int maxsprite = 0;
        for (int i = 0;i<300;i++) {
          if (sprite_scan == null || !sprite_scan.startsWith("SPRITE")) break;
          sprite_scan = sprite_scan.replaceAll("\\s+", " ");
          String[] parse = sprite_scan.split("\\s");
          maxsprite = Integer.parseInt(parse[1]);
          
          sprite_scan = gani_reader.readLine();
        }
        
        sprites = new String[maxsprite+1][5];

        gani_reader.reset();
        
        boolean foundframe = false;
        
        while (gani_line != null) {
          gani_line = gani_line.replaceAll("\\s+", " ");
          if (gani_line.startsWith("SPRITE")) {
            String[] parse = gani_line.split("\\s");
            
            int spritenumber = Integer.parseInt(parse[1]);
            
            if (spritenumber < 0 || spritenumber > sprites.length-1) {
              gani_line = gani_reader.readLine();
              continue;
            }
          
            String[] temparr = {parse[2],parse[3],parse[4],parse[5],parse[6]};
            
            sprites[spritenumber] = temparr;
          } else if (gani_line.startsWith("SINGLEDIRECTION")) {
            dir = 0;
          } else if (gani_line.startsWith("ANI")) {
            for (int i = 0;i<dir;i++) {
              gani_line = gani_reader.readLine();
            }
            foundframe = true;
          } else if (foundframe == true) {
            gani_line = gani_line.replaceAll("\\s+"," ");
            gani_line = gani_line.replaceAll(",\\s+",",");
            gani_line = gani_line.trim();
            
            String[] tokensprites = gani_line.split(",");
            for (String i : tokensprites) {
              String[] tokenspritedata = i.split(" ");
              if (Integer.parseInt(tokenspritedata[0]) < 0) continue;
              int[] temparr = {Integer.parseInt(tokenspritedata[0]),
                               Integer.parseInt(tokenspritedata[1]),
                               Integer.parseInt(tokenspritedata[2])
              };
              rendersprites.add(temparr);
            }
            
            break;
          }
          gani_line = gani_reader.readLine();
        }
        
        gani_reader.close();
        
        // attrs = {gani,#c0,#c1,#c2,#c3,#c4,#P1,#P2,#P3,#1,#2,#3,#8,param1,dir};
        if (attr[6]  != null) img_attr1    = attr[1];
        if (attr[7]  != null) img_attr2    = attr[7];
        if (attr[8]  != null) img_attr3    = attr[8];
        if (attr[9]  != null) img_sword    = attr[9];
        if (attr[10] != null) img_shield   = attr[10];
        if (attr[11] != null) img_head     = attr[11];
        if (attr[12] != null) img_body     = attr[12];
        if (attr[13] != null) img_param1   = attr[13];
        
        if (attr[1] != null) color_skin    = attr[1];
        if (attr[2] != null) color_coat    = attr[2];
        if (attr[3] != null) color_sleeves = attr[3];
        if (attr[4] != null) color_shoes   = attr[4];
        if (attr[5] != null) color_belt    = attr[5];
        
        // Find largest sprite to create appropriate image buffer
        int sprite_minX = 0;
        int sprite_minY = 0;
        int sprite_maxW = 1;
        int sprite_maxH = 1;
        
        for (int i[] : rendersprites) {
          int sprite_X = i[1];
          int sprite_Y = i[2];
          
          if (sprite_X < sprite_minX) sprite_minX = sprite_X;
          if (sprite_Y < sprite_minY) sprite_minY = sprite_Y;
        }
        
        if (sprite_minX < 0) ganiOffsetx = sprite_minX;
        if (sprite_minY < 0) ganiOffsety = sprite_minY;

        for (int i[] : rendersprites) {
          int sprite_X = i[1];
          int sprite_Y = i[2];
          
          int sprite_W = Integer.parseInt(sprites[i[0]][3]) + Math.abs(sprite_minX) + sprite_X;
          int sprite_H = Integer.parseInt(sprites[i[0]][4]) + Math.abs(sprite_minY) + sprite_Y;
          
          if (sprite_W > sprite_maxW) sprite_maxW = sprite_W;
          if (sprite_H > sprite_maxH) sprite_maxH = sprite_H;
        }
        
        if (sprite_maxW < 48) sprite_maxW = 48;
        if (sprite_maxH < 48) sprite_maxH = 48;
        
        BufferedImage ganiImage = new BufferedImage(sprite_maxW,sprite_maxH, BufferedImage.TYPE_INT_ARGB);
        Graphics g = ganiImage.getGraphics();
        
        boolean isbody = false;
        for (int i[] : rendersprites) {
          isbody = false;
          String sprite_img   = sprites[i[0]][0];
          int    sprite_drawx = i[1] - ganiOffsetx;
          int    sprite_drawy = i[2] - ganiOffsety;
          if (sprite_img.equals("SPRITES")) sprite_img = img_sprites;
          else if (sprite_img.equals("BODY")) {
            isbody = true;
            sprite_img = img_body;
          } else if (sprite_img.equals("HEAD")) sprite_img = img_head;
          else if (sprite_img.equals("SWORD")) sprite_img = img_sword;
          else if (sprite_img.equals("SHIELD")) sprite_img = img_shield;
          else if (sprite_img.equals("ATTR1")) sprite_img = img_attr1;
          else if (sprite_img.equals("ATTR2")) sprite_img = img_attr2;
          else if (sprite_img.equals("ATTR3")) sprite_img = img_attr3;
          else if (sprite_img.equals("PARAM1")) sprite_img = img_param1;
          
          //System.out.println(getImageLocation(sprite_img));
          
          try { 
            if (sprite_img == null) continue;
            BufferedImage sprite_render = ImageIO.read(new File(getImageLocation(sprite_img)));
            if (sprite_render == null) continue;
            
            int sprite_sx = Integer.parseInt(sprites[i[0]][1]);
            int sprite_sy = Integer.parseInt(sprites[i[0]][2]);
            int sprite_sw = Integer.parseInt(sprites[i[0]][3]);
            int sprite_sh = Integer.parseInt(sprites[i[0]][4]);
            
            if (isbody) {
              //transparent = 008400;
              //skin = ffad6b;
              //coat = ffffff;
              //belt = 0000ff;
              //sleeve = ff0000;
              //shoes = ce1829;
              int bodycolors[][] = {
                  {0x008400,0xffad6b,0xffffff,0x0000ff,0xff0000,0xce1829},
                  {0x300000,0x400000,0x500000,0x600000,0x700000,0x800000}
              };
              
              //if (attr[1] != null) color_skin    = attr[1];
              //if (attr[2] != null) color_coat    = attr[2];
              //if (attr[3] != null) color_sleeves = attr[3];
              //if (attr[4] != null) color_shoes   = attr[4];
              //if (attr[5] != null) color_belt    = attr[5];
              
              for (int j=0;j<6;j++) {
                sprite_render = TransformColorToNewColor(sprite_render,new Color(bodycolors[0][j]),new Color(bodycolors[1][j]));
              }

              int newColor = 0;
              //sprite_render = TransformColorToNewColor(sprite_render,new Color(bodycolors[1][0]),new Color(0,0,0,0));
              newColor = defaultcolorshex[findColornameValue(defaultcolors,color_skin)];
              sprite_render = TransformColorToNewColor(sprite_render,new Color(bodycolors[1][1]),new Color(newColor));
              newColor = defaultcolorshex[findColornameValue(defaultcolors,color_coat)];
              sprite_render = TransformColorToNewColor(sprite_render,new Color(bodycolors[1][2]),new Color(newColor));
              newColor = defaultcolorshex[findColornameValue(defaultcolors,color_belt)];
              sprite_render = TransformColorToNewColor(sprite_render,new Color(bodycolors[1][3]),new Color(newColor));
              newColor = defaultcolorshex[findColornameValue(defaultcolors,color_sleeves)];
              sprite_render = TransformColorToNewColor(sprite_render,new Color(bodycolors[1][4]),new Color(newColor));
              newColor = defaultcolorshex[findColornameValue(defaultcolors,color_shoes)];
              sprite_render = TransformColorToNewColor(sprite_render,new Color(bodycolors[1][5]),new Color(newColor));
              /*
              int[] bodypixels = new int[sprite_render.getWidth()*sprite_render.getHeight()];
              sprite_render.getRGB(0,0,sprite_render.getWidth(),sprite_render.getHeight(),
                  bodypixels,0,sprite_render.getWidth());
                  */
            }
            
            //g.setColor(new Color(0,0,255,50));
            //g.fillRect(0,0,ganiImage.getWidth(),ganiImage.getHeight());
            
            g.drawImage(sprite_render,
                        sprite_drawx,sprite_drawy,
                        sprite_drawx+sprite_sw,sprite_drawy+sprite_sh,
                        sprite_sx,sprite_sy,sprite_sx + sprite_sw, sprite_sy + sprite_sh,
                        null);
            
            //System.out.println(sprite_img + " @ " + sprite_drawx + " : " + sprite_drawy + " | " + sprite_maxW + " : " + sprite_maxH);
            
          } catch (FileNotFoundException e) {
            getListener().sendMessage("Error: Can't find image: " + sprite_img);
          } catch (IOException e) {
            getListener().sendMessage("Error: Can't find image: " + sprite_img);
          }
          
        }
        return ganiImage;
        
      } catch (FileNotFoundException e) {
      } catch (IOException e) {
      }
      return null;
    }
    
    private int findColornameValue(String[] a,String s) {
      for (int i=0;i<a.length;i++) {
        if (a[i].equals(s)) {
          return i;
        }
      }
      return 0;
    }
    
    private int findInt(String s) {
      try {
        int return_val = Integer.parseInt(s);
        return return_val;
      } catch (java.lang.NumberFormatException e) {
        try {
          ScriptEngineManager mgr = new ScriptEngineManager();
          ScriptEngine engine = mgr.getEngineByName("JavaScript");
          double math_val = (Double) engine.eval(s);
          int return_val = (int)math_val;
          return return_val;
        } catch (ScriptException e1) {
          e1.printStackTrace();
        }
      }
      getListener().sendMessage("Warning: Could not parse: " + s);
      return 0;
    }
    
    private double findDouble(String s) {
      try {
        double return_val = Double.parseDouble(s);
        return return_val;
      } catch (java.lang.NumberFormatException e) {
        try {
          ScriptEngineManager mgr = new ScriptEngineManager();
          ScriptEngine engine = mgr.getEngineByName("JavaScript");
          double math_val = (Double) engine.eval(s);
          return math_val;
        } catch (ScriptException e1) {
          e1.printStackTrace();
        }
      }
      getListener().sendMessage("Warning: Could not parse: " + s);
      return 0.0;
    }
    
    // NOT MY(Dusty) WORK!
    private BufferedImage TransformColorToNewColor(BufferedImage image, Color c1, Color c2) {
      // Primitive test, just an example
      final int r1 = c1.getRed();
      final int g1 = c1.getGreen();
      final int b1 = c1.getBlue();
      final int r2 = c2.getRed();
      final int g2 = c2.getGreen();
      final int b2 = c2.getBlue();
      final int a2 = c2.getAlpha();
      ImageFilter filter = new RGBImageFilter() {
        public final int filterRGB(int x, int y, int argb) {
          int a = 255;
          if (a2 > 0) a = (argb & 0xFF000000) >> 24;
          else a = 0;
          int r = (argb & 0xFF0000) >> 16;
          int g = (argb & 0xFF00) >> 8;
          int b = (argb & 0xFF);

          // Check if this color matches c1.  If not, it is not our target color.
          // Don't bother with it in this case.
          if (r != r1 || g != g1 || b != b1)
            return argb;

          // Set r, g, and b to our new color.  Bit-shift everything left to get it
          // ready for re-packing.
          if (a2 > 0) a = a << 24;
          r = r2 << 16;
          g = g2 << 8;
          b = b2;

          // Re-pack our colors together with a bitwise OR.
          //return a | r | g | b;
          return a | r | g | b;
        }
      };

      ImageProducer ip = new FilteredImageSource(image.getSource(), filter);
      //BufferedImage test = new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_INT_ARGB);
      
      //BufferedImage new_renderbuffer = new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_INT_ARGB);
      //Graphics g = ganiImage.getGraphics();
      
      Image new_renderimage = Toolkit.getDefaultToolkit().createImage(ip);
      
      BufferedImage new_renderbuffer = toBufferedImage(new_renderimage);
      
      /*
      try {
        if (ImageIO.createImageInputStream(Toolkit.getDefaultToolkit().createImage(ip)) == null) System.out.println("Test!");
        new_render = ImageIO.read(ImageIO.createImageInputStream(Toolkit.getDefaultToolkit().createImage(ip)));
      } catch (IOException e) {
        e.printStackTrace();
      }
      */
      
      if (new_renderbuffer == null) return null;
      
      return new_renderbuffer;
      //return Toolkit.getDefaultToolkit().createImage(ip);
    }
    
    public static BufferedImage toBufferedImage(Image image) {
      if (image instanceof BufferedImage) {
          return (BufferedImage)image;
      }

      // This code ensures that all the pixels in the image are loaded
      image = new ImageIcon(image).getImage();

      // Determine if the image has transparent pixels; for this method's
      // implementation, see Determining If an Image Has Transparent Pixels
      boolean hasAlpha = true;

      // Create a buffered image with a format that's compatible with the screen
      BufferedImage bimage = null;
      GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
      try {
          // Determine the type of transparency of the new buffered image
          int transparency = Transparency.OPAQUE;
          if (hasAlpha) {
              transparency = Transparency.BITMASK;
          }

          // Create the buffered image
          GraphicsDevice gs = ge.getDefaultScreenDevice();
          GraphicsConfiguration gc = gs.getDefaultConfiguration();
          bimage = gc.createCompatibleImage(
              image.getWidth(null), image.getHeight(null), transparency);
      } catch (HeadlessException e) {
          // The system does not have a screen
      }

      if (bimage == null) {
          // Create a buffered image using the default color model
          int type = BufferedImage.TYPE_INT_RGB;
          if (hasAlpha) {
              type = BufferedImage.TYPE_INT_ARGB;
          }
          bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
      }

      // Copy image to buffered image
      Graphics g = bimage.createGraphics();

      // Paint the image onto the buffered image
      g.drawImage(image, 0, 0, null);
      g.dispose();

      return bimage;
  }
}