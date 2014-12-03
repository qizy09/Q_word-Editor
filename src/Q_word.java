/**
 * @(#)Q_word.java    
 * 
 * Copyright (c) 2010 Thss. All Rights Reserved.
 * 
 * Small text editor that supports only one font.
 * 
 * 
 * Author: Qi
 * modification, are permitted provided that the following conditions are met:
 * Version: 2.3.2
 * 
 * Last Modified: 2010/09/10
 * 
 * 
 */

//import java.awt.*;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Frame;
import java.awt.Event;
import java.awt.Container;
import java.awt.Component;
import java.awt.FileDialog;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Dimension;
//import java.awt.event.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
//import java.beans.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
//import java.io.*;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
//import java.net.URL;
import java.net.URL;
//import java.util.*;
import java.util.ResourceBundle;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Date;
import java.util.Timer;
import java.util.Calendar;
import java.util.TimerTask;
//import javax.swing.text.*;
import javax.swing.text.PlainDocument;
import javax.swing.text.Document;
import javax.swing.text.Segment;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
//import javax.swing.undo.*;
import javax.swing.undo.UndoManager;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
//import javax.swing.event.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.event.CaretListener;
import javax.swing.event.CaretEvent;
//import javax.swing.*;
import javax.swing.JPanel;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.text.TextAction;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.UIManager;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JToolBar;
import javax.swing.JTextArea;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.BoxLayout;
import javax.swing.SwingUtilities;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.WindowConstants;
import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.JCheckBox;

import java.text.SimpleDateFormat;



class Q_word extends JPanel 
{

    private static ResourceBundle resources;
    private final static String EXIT_AFTER_PAINT = new String("-exit");
    private static boolean exitAfterFirstPaint;
    
    static 
    {
        try 
        {
            resources = ResourceBundle.getBundle("resources.Q_word", 
                                                 Locale.ENGLISH);
        } 
        catch (MissingResourceException mre) 
        {
            System.err.println("resources/Q_word.properties not found");
            System.exit(1);
        }
    }


    Q_word() 
    {
        super(true);

        // Force SwingSet to come up in the Cross Platform L&F
        try 
        {
            // Choose System L&F
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } 
        catch (Exception e) 
        {
            System.err.println("Error loading L&F: " + e);
        }
        //make the background highlight & shadow
        setBorder(BorderFactory.createEtchedBorder());
        setLayout(new BorderLayout());

        // create the embedded JTextComponent for the 
        editor = createEditor();

        editor.getDocument().addUndoableEditListener(undoHandler);

        // install the command table, for redo & undo functions
        commands = new Hashtable();        
        Action[] actions = TextAction.augmentList(editor.getActions(), defaultActions);
        for (int i = 0; i < actions.length; i++) 
        {
            Action a = actions[i];
                commands.put(a.getValue(Action.NAME), a);
        }
    
        scroller = new JScrollPane();
        scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );        
        JViewport port = scroller.getViewport();
        port.add(editor);

        try 
        {
            String vpFlag = resources.getString("ViewportBackingStore");
            Boolean bs = Boolean.valueOf(vpFlag);
            port.setScrollMode(JViewport.BLIT_SCROLL_MODE);
        }
        catch (MissingResourceException mre) 
        {
        // just use the viewport default
        }

        menuItems = new Hashtable();
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());    
        panel.add("North",createToolbar());
        panel.add("Center", scroller);
        add("Center", panel);
        add("South", createStatusbar());
    }

    public static void main(String[] args) 
    {
        try 
        {
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame.setTitle(resources.getString("Title"));
            frame.setBackground(Color.lightGray);
            frame.getContentPane().setLayout(new BorderLayout());
            Q_word qword = new Q_word();
            URL mImage = qword.getResource("Icon");
            frame.setIconImage(Toolkit.getDefaultToolkit().createImage(mImage));
            Container c = frame.getContentPane();
            c.add("Center", qword);
            c.add("North", qword.createMenubar());
            //frame.setJMenuBar();
            frame.addWindowListener(new AppCloser());
            frame.pack();
            frame.setSize(500, 600);
            frame.setVisible(true);
        } 
        catch (Throwable t) 
        {
        }
    }

    /**
     * Fetch the list of actions supported by this
     * editor.  It is implemented to return the list
     * of actions supported by the embedded JTextComponent
     * augmented with the actions defined locally.
     */

    public Action[] getActions() 
    {
        return TextAction.augmentList(editor.getActions(), defaultActions);
    }

    /**
     * Create an editor to represent the given document.  
     */
    protected JTextComponent createEditor() 
    {
        JTextArea c = new JTextArea();
        c.setDragEnabled(true);
        c.setFont(new Font("monospaced", Font.PLAIN, 12));
        //EditorColumns = (int) (c.getPreferredSize().getWidth() / ((new Font("monospaced", Font.PLAIN, 12)).getSize2D()) * 72);
        c.addCaretListener(new CaretListener() 
        { 
            public void caretUpdate(final CaretEvent e) 
            {
                SaveStaus = false;
            }
        });
        //System.out.println(EditorColumns);
        return c;
    }

    /** 
     * Fetch the editor contained in this panel
     */
    protected JTextComponent getEditor() 
    {
        return editor;
    }
    
    protected int getTextLine()
    {
        JTextArea jta = (JTextArea)getEditor();
        int loc = (jta.getCaretPosition());
        return loc;
    }

    protected int  getTextColumn()
    {
        JTextArea jta = (JTextArea)getEditor();
        int loc = (jta.getCaretPosition());
        return loc;
    }

    /**
     * To shutdown when run as an application.  This is a
     * fairly lame implementation.   A more self-respecting
     * implementation would at least check to see if a save
     * was needed.
     */
    protected static final class AppCloser extends WindowAdapter 
    {
        public void windowClosing(WindowEvent e) 
        {            
            if (SaveStaus)
            {
                System.exit(0);
            }
            else
            {
                try 
                {
                    JFrame ap = (JFrame)e.getSource();
                    int flg = JOptionPane.showConfirmDialog(ap, "Do you really want to quit without saving?",
                                            "Quit Q_word", JOptionPane.YES_NO_OPTION
                                            , JOptionPane.INFORMATION_MESSAGE, (new ImageIcon(getClass().getResource(resources.getString("IconC")))) );
                    if (flg == 0)
                        System.exit(0);
                    else
                        ap.setVisible(true);
                } 
                catch (MissingResourceException mre) 
                {
                    System.err.println(mre);
                }
            }
        }
    }

    /**
     * Find the hosting frame, for the file-chooser dialog.
     */
    protected Frame getFrame() 
    {
        for (Container p = getParent(); p != null; p = p.getParent()) 
        {
            if (p instanceof Frame) 
            {
                return (Frame) p;
            }
        }
        return null;
    }

    /**
     * This is the hook through which all menu items are
     * created.  It registers the result with the menuitem
     * hashtable so that it can be fetched with getMenuItem().
     * @see #getMenuItem
     */
    protected JCheckBoxMenuItem createMenuItem(String cmd) 
    {
        JCheckBoxMenuItem mItem = new JCheckBoxMenuItem(getResourceString(cmd + labelSuffix));
        URL mImage = getResource(cmd + imageSuffix);
        if (mImage != null) 
        {
            mItem.setHorizontalTextPosition(JButton.RIGHT);
            Image img = Toolkit.getDefaultToolkit().createImage(mImage);
            ImageIcon iIcon = new ImageIcon();
            iIcon.setImage(img.getScaledInstance(24, 24, Image.SCALE_DEFAULT));            
            mItem.setIcon(iIcon);
        }
        String aStr = getResourceString(cmd + actionSuffix);
        if (aStr == null) 
        {
            aStr = cmd;
        }
        mItem.setActionCommand(aStr);
        Action a = getAction(aStr);
        if (a != null) 
        {
            mItem.addActionListener(a);
            a.addPropertyChangeListener(createActionChangeListener(mItem));
            mItem.setEnabled(a.isEnabled());
        } 
        else 
        {
            mItem.setEnabled(false);
        }
        
        menuItems.put(cmd, mItem);
        if (cmd.equals("statusbar"))
            mItem.setState(true);
        else
            mItem.setState(false);
                
        return mItem;
    }

    /**
     * Fetch the menu item that was created for the given
     * command.
     * @param cmd  Name of the action.
     * @returns item created for the given command or null
     *  if one wasn't created.
     */
    protected JCheckBoxMenuItem getMenuItem(String cmd) 
    {
        return (JCheckBoxMenuItem) menuItems.get(cmd);
    }

    protected Action getAction(String cmd) 
    {
        return (Action) commands.get(cmd);
    }

    protected String getResourceString(String name) 
    {
        String str;
        try 
        {
            str = resources.getString(name);
        } 
        catch (MissingResourceException mre) 
        {
            str = null;
        }
        return str;
    }

    protected URL getResource(String key)
    {
        String name = getResourceString(key);
        if (name != null) 
        {
            URL url = this.getClass().getResource(name);
            return url;
        }
        return null;
    }

    protected Container getToolbar() 
    {
        return toolbar;
    }

    protected JMenuBar getMenubar() 
    {
        return menubar;
    }

    /**
     * Create a status bar
     */
    protected Component createStatusbar() 
    {
        status = new StatusBar();
        return status;
    }

    protected void resetUndoManager() 
    {
        undo.discardAllEdits();
        undoAction.update();
        redoAction.update();
    }

    /**
     * Create the toolbar.  By default this reads the 
     * resource file for the definition of the toolbar.
     */
    private Component createToolbar() 
    {
        toolbar = new JToolBar();
        String[] toolKeys = tokenize(getResourceString("toolbar"));
        for (int i = 0; i < toolKeys.length; i++) 
        {
            if (toolKeys[i].equals("-")) 
            {
                toolbar.addSeparator();//(Box.createHorizontalStrut(5));
            } 
            else 
            {
                toolbar.add(createTool(toolKeys[i]));
            }
        }
        toolbar.add(Box.createHorizontalGlue());
        toolbar.setFloatable(false);         
        return toolbar;
    }

    /**
     * Hook through which every toolbar item is created.
     */
    protected Component createTool(String key) 
    {
        return createToolbarButton(key);
    }

    /**
     * Create a button to go inside of the toolbar.  By default this
     * will load an image resource.  The image filename is relative to
     * the classpath (including the '.' directory if its a part of the
     * classpath), and may either be in a JAR file or a separate file.
     * 
     * @param key The key in the resource file to serve as the basis
     *  of lookups.
     */
    protected JButton createToolbarButton(String key) 
    {
        URL url = getResource(key + imageSuffix);
				Image img = Toolkit.getDefaultToolkit().createImage(url);
				ImageIcon iIcon = new ImageIcon();
        iIcon.setImage(img.getScaledInstance(24, 24, Image.SCALE_DEFAULT));            
        JButton b = new JButton(iIcon)
        {
            public float getAlignmentY() 
            { 
                return 0.5f; 
            }
        };
        b.setRequestFocusEnabled(false);
        b.setMargin(new Insets(1,1,1,1));

        String aStr = getResourceString(key + actionSuffix);
        if (aStr == null) 
        {
            aStr = key;
        }
        Action a = getAction(aStr);
        if (a != null) 
        {
            b.setActionCommand(aStr);
            b.addActionListener(a);
        } 
        else 
        {
            b.setEnabled(false);
        }

        String tip = getResourceString(key + tipSuffix);
        if (tip != null) 
        {
            b.setToolTipText(tip);
        }
        b.setFocusable(false); 
        return b;
    }

    /**
     * Take the given string and chop it up into a series
     * of strings on whitespace boundaries.  This is useful
     * for trying to get an array of strings out of the
     * resource file.
     */
    protected String[] tokenize(String input) 
    {
        Vector v = new Vector();
        StringTokenizer t = new StringTokenizer(input);
        String cmd[];

        while (t.hasMoreTokens())
            v.addElement(t.nextToken());
        
        cmd = new String[v.size()];
        for (int i = 0; i < cmd.length; i++)
            cmd[i] = (String) v.elementAt(i);

        return cmd;
    }

    /**
     * Create the menubar for the app.  By default this pulls the
     * definition of the menu from the associated resource file. 
     */
    protected JMenuBar createMenubar() 
    {
//        JCheckBoxMenuItem mItem;
        JMenuBar mb = new JMenuBar();

        String[] menuKeys = tokenize(getResourceString("menubar"));
        for (int i = 0; i < menuKeys.length; i++) 
        {
            JMenu m = createMenu(menuKeys[i]);
            if (m != null) 
            {
                mb.add(m);
            }
        }
        this.menubar = mb;
        return mb;
    }

    /**
     * Create a menu for the app.  By default this pulls the
     * definition of the menu from the associated resource file.
     */
    protected JMenu createMenu(String key) 
    {
        String[] itemKeys = tokenize(getResourceString(key));
        JMenu menu = new JMenu(getResourceString(key + "Label"));
        for (int i = 0; i < itemKeys.length; i++) 
        {
            if (itemKeys[i].equals("-")) 
            {
                menu.addSeparator();
            } 
            else 
            {
                JCheckBoxMenuItem mItem = createMenuItem(itemKeys[i]);
                menu.add(mItem);
                mItem.setMnemonic(getResourceString(itemKeys[i] + "Mnemonic").charAt(0));
                mItem.setAccelerator (KeyStroke.getKeyStroke((getResourceString(itemKeys[i] + "Accelerator"))));
            }
        }
        menu.setMnemonic(getResourceString(key + "Mnemonic").charAt(0));
        return menu;
    }

    protected void FindNext(Component par, boolean flg)
    {
        JTextArea jta = (JTextArea)editor;
        if (flg)
        {
            int index = jta.getText().indexOf(SelText, jta.getCaretPosition());
            if (index > -1)
            {
                jta.select(index, index + SelText.length());
                return;
            }
            else
            {
                index = jta.getText().indexOf(SelText);
                if (index > -1)
                {
                    jta.select(index, index + SelText.length());
                    return;
                }
            }
        }
        else
        {
            int index = jta.getText().toLowerCase().indexOf(SelText.toLowerCase(), jta.getCaretPosition());
            if (index > -1)
            {
                jta.select(index, index + SelText.length());
                return;
            }
            else
            {
                index = jta.getText().toLowerCase().indexOf(SelText.toLowerCase());
                if (index > -1)
                {
                    jta.select(index, index + SelText.length());
                    return;
                }
            }
        }
        JOptionPane.showMessageDialog(par, "        No \"" + SelText + "\" found.", "FIND", 
                                        JOptionPane.INFORMATION_MESSAGE, (new ImageIcon(getResource("findImage"))));
    }
    
    protected void replaceall(Component par, boolean flg)
    {
        JTextArea jta = (JTextArea)editor;
        int cnt = 0, index;
        if (flg)
        {
            index = jta.getText().indexOf(SelText);
            while (index > -1)
            {
                jta.select(index, index + SelText.length());
                jta.replaceSelection(ReplaceText);
                index = jta.getText().indexOf(SelText, index + ReplaceText.length());
                cnt++;
            }
        }
        else
        {
            index = jta.getText().toLowerCase().indexOf(SelText.toLowerCase());
            while (index > -1)
            {
                jta.select(index, index + SelText.length());
                jta.replaceSelection(ReplaceText);
                index = jta.getText().toLowerCase().indexOf(SelText.toLowerCase(), index + ReplaceText.length());
                cnt++;
            }
        }
        JOptionPane.showMessageDialog(par, "        Totaly " + cnt + " replaced.", "REPLACE", 
                                        JOptionPane.INFORMATION_MESSAGE, (new ImageIcon(getResource("replaceImage"))));
    }

    protected PropertyChangeListener createActionChangeListener(JCheckBoxMenuItem mItem) 
    {
        return new ActionChangedListener(mItem);
    }

    private class ActionChangedListener implements PropertyChangeListener 
    {
        JCheckBoxMenuItem mItem;
        
        ActionChangedListener(JCheckBoxMenuItem mi) 
        {
            super();
            this.mItem = mi;
        }
        
        public void propertyChange(PropertyChangeEvent e) 
        {
            String propertyName = e.getPropertyName();
            if (e.getPropertyName().equals(Action.NAME)) 
            {
                String text = (String) e.getNewValue();
                mItem.setText(text);
            } 
            else if (propertyName.equals("enabled")) 
            {
                Boolean enabledState = (Boolean) e.getNewValue();
                mItem.setEnabled(enabledState.booleanValue());
            }
        }
    }

    private JTextComponent editor;
    private Hashtable commands;
    private Hashtable menuItems;
    private JMenuBar menubar;
    private JToolBar toolbar;
    private JComponent status;
    private JFrame elementTreeFrame;
    private JScrollPane scroller;

    protected FileDialog fileDialog;

    /**
     * Listener for the edits on the current document.
     */
    protected UndoableEditListener undoHandler = new UndoHandler();

    /** UndoManager that we add edits to. */
    protected UndoManager undo = new UndoManager();

    private static boolean SaveStaus = true;
    private static boolean MatchCase = false;
    private static String SelText = new String("");
    private static String ReplaceText = new String("");
    private static Component parent1 = null;
    
    /**
     * Suffix applied to the key used in resource file
     * lookups for an image.
     */
    public static final String imageSuffix = "Image";

    /**
     * Suffix applied to the key used in resource file
     * lookups for a label.
     */
    public static final String labelSuffix = "Label";

    /**
     * Suffix applied to the key used in resource file
     * lookups for an action.
     */
    public static final String actionSuffix = "Action";

    /**
     * Suffix applied to the key used in resource file
     * lookups for tooltip text.
     */
    public static final String tipSuffix = "Tooltip";

    public static final String aboutMsg = "Q_Word text editor...\n\n\n\nQ_WORD TEXT EDITOR is a simple text editor \n" +
                                        "DESIGNED FOR YOU & FOR ME.\n\n  Author     :  Qi.\n  Version    :  2.3.2.\n  " +
                                        "Copyright :  2010 Thss.\n\n\n    Q_word...\n\n";
    public static final String openAction = "open";
    public static final String newAction  = "new";
    public static final String saveAction = "save";
    public static final String save_asAction = "save_as";
    public static final String exitAction = "exit";

    class UndoHandler implements UndoableEditListener 
    {
    /**
     * Messaged when the Document has created an edit, the edit is
     * added to <code>undo</code>, an instance of UndoManager.
     */
        public void undoableEditHappened(UndoableEditEvent e) 
        {
            undo.addEdit(e.getEdit());
            undoAction.update();
            redoAction.update();
        }
    }

    /**
     * StatusBar
     */
    class StatusBar extends JPanel
    {
        JPanel timPanel = new JPanel(), numPanel = new JPanel();
        JLabel timLabel, locLabel;
    
        public StatusBar() 
        {
            super();
            setLayout(new GridLayout(1,2));
            timLabel = new JLabel("Loading time.");
            timPanel.add(timLabel);
            locLabel = new JLabel("Loading location", JLabel.RIGHT);
            numPanel.add(locLabel);
            this.add(timPanel);
            //this.addSeparator();
            this.add(numPanel);
            this.setVisible(true);
            reLable();
         }        
         
        public void paint(Graphics g) 
        {
            super.paint(g);
        }
        
        public class TimerTaskDemo extends TimerTask
        {
            SimpleDateFormat df = new SimpleDateFormat( "yyyy-MM-dd   HH:mm:ss ");
            String str;
            public void run()
            {
                str = df.format(Calendar.getInstance().getTime());
                timLabel.setText("Time: " + str);
                locLabel.setText("          SaveStaus: " + SaveStaus + ".");
//                System.out.println("Line " + getTextLine() + ", Column " + getTextColumn());
            }
        }
        
        private void reLable()
        { 
            Date d = new Date();
            Timer tim = new Timer();
            tim.schedule(new TimerTaskDemo(), d, 200);  
        }
    }

    // --- action implementations -----------------------------------

    private UndoAction undoAction = new UndoAction();
    private RedoAction redoAction = new RedoAction();


    /**
     * Actions defined by the Q_word class
     */
    private Action[] defaultActions = 
    {
        new NewAction(),
        new OpenAction(),
        new SaveAction(),
        new Save_AsAction(),
        new ExitAction(),
        redoAction,
        undoAction,
        new AboutAction(),
        new SupportAction(),
        new select_allAction(),
        new time_dateAction(),
        new statusbarAction(),
        new line_wrapAction(),
        new findAction(),
        new find_nextAction(),
        new replaceAction()
    };

    class UndoAction extends AbstractAction 
    {
        public UndoAction() 
        {
            super("Undo");
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) 
        {
            ((JCheckBoxMenuItem)e.getSource()). setState(false);
            try 
            {
                undo.undo();
            } 
            catch (CannotUndoException ex) 
            {
                System.out.println("Unable to undo: " + ex);
                ex.printStackTrace();
            }
            update();
            redoAction.update();
        }

        protected void update() 
        {
            if(undo.canUndo()) 
            {
                setEnabled(true);
                putValue(Action.NAME, undo.getUndoPresentationName());
            }
            else 
            {
                setEnabled(false);
                putValue(Action.NAME, "Undo");
            }
        }
    }

    class AboutAction extends AbstractAction 
    {
        public AboutAction() 
        {
            super("About");
            setEnabled(true);
        }

        public void actionPerformed(ActionEvent e) 
        {
            ((JCheckBoxMenuItem)e.getSource()). setState(false);
            if (e.getActionCommand().equals("About"))
            {
                Component par = (JCheckBoxMenuItem)e.getSource();
                JOptionPane.showMessageDialog(par, aboutMsg,  "About Q_word", 
                                              JOptionPane.INFORMATION_MESSAGE, (new ImageIcon(getResource("Icon"))));
            }
        }
    }

    class select_allAction extends AbstractAction 
    {
        public select_allAction() 
        {
            super("select_all");
            setEnabled(true);
        }

        public void actionPerformed(ActionEvent e) 
        {
            ((JCheckBoxMenuItem)e.getSource()). setState(false);
            if (e.getActionCommand().equals("select_all"))
            {
                editor.selectAll();
            }
        }
    }

    class replaceAction extends AbstractAction 
    {
        public replaceAction() 
        {
            super("replace");
            setEnabled(true);
        }

        public void actionPerformed(ActionEvent e) 
        {
            ((JCheckBoxMenuItem)e.getSource()). setState(false);
            if (e.getActionCommand().equals("replace"))
            {
                parent1 = getFrame();
                String str = editor.getSelectedText();
                if ((str != null) && (str.length() > 0))
                    SelText = str;
                new replaceBox();
                parent1 = null;
            }
        }
    }
    
    class findAction extends AbstractAction 
    {
        public findAction() 
        {
            super("find");
            setEnabled(true);
        }

        public void actionPerformed(ActionEvent e) 
        {
            ((JCheckBoxMenuItem)e.getSource()). setState(false);
            if (e.getActionCommand().equals("find"))
            {
                parent1 = getFrame();
                if ((SelText.length() < 1) && (editor.getSelectedText() != null))
                    SelText = editor.getSelectedText();
                //System.out.println(1 + SelText + 1);
                new findBox();
                parent1 = null;
            }
        }
    }

    class find_nextAction extends AbstractAction 
    {
        public find_nextAction() 
        {
            super("find_next");
            setEnabled(true);
        }

        public void actionPerformed(ActionEvent e) 
        {
            ((JCheckBoxMenuItem)e.getSource()). setState(false);
            String str = editor.getSelectedText();
            if ((str != null) && (str.length() > 0))
                SelText = str;
            FindNext(getFrame(), MatchCase);
        }
    }

    class SupportAction extends AbstractAction 
    {
        public SupportAction() 
        {
            super("Support");
            setEnabled(true);
        }

        public void actionPerformed(ActionEvent e) 
        {
            ((JCheckBoxMenuItem)e.getSource()). setState(false);

            if (e.getActionCommand().equals("Support"))
            {
                try 
                {
                    Component par = (JCheckBoxMenuItem)e.getSource();
                    Runtime.getRuntime().exec("cmd.exe /c start help.CHM");
                    JOptionPane.showMessageDialog(par, "SORRY!\n\n   No support document exists yet.\n   This document is false.\n\n",  
                                                "Q_word Support Message", 
                                                JOptionPane.INFORMATION_MESSAGE, (new ImageIcon(getResource("IconC"))));
                   
                } 
                catch (IOException e2)
                {
                    e2.printStackTrace();
                }
            }
        }
    }

    class statusbarAction extends AbstractAction 
    {
        public  statusbarAction() 
        {
            super("statusbar");
            setEnabled(true);
        }

        public void actionPerformed(ActionEvent e) 
        {
            status.setVisible(((JCheckBoxMenuItem)e.getSource()).getState());
        }
    }

    class line_wrapAction extends AbstractAction 
    {
        public  line_wrapAction() 
        {
            super("line_wrap");
            setEnabled(true);
        }

        public void actionPerformed(ActionEvent e) 
        {
            if (((JCheckBoxMenuItem)e.getSource()).getState())
            {
                scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                ((JTextArea)editor).setLineWrap(true);
            }
            else
            {
                scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
                ((JTextArea)editor).setLineWrap(false);
            }
        }
    }

    class time_dateAction extends AbstractAction 
    {
        public time_dateAction() 
        {
            super("time_date");
            setEnabled(true);
        }

        public void actionPerformed(ActionEvent e) 
        {
            ((JCheckBoxMenuItem)e.getSource()). setState(false);
            String str = editor.getText();
            int pos = editor.getCaretPosition();
            SimpleDateFormat df = new SimpleDateFormat( "yyyy-MM-dd   HH:mm:ss ");
            String st = str.substring(0, pos) + df.format(Calendar.getInstance().getTime()) + str.substring(pos);
            editor.setText(st);
        }
    }

    class RedoAction extends AbstractAction 
    {
        public RedoAction() 
        {
            super("Redo");
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) 
        {
            ((JCheckBoxMenuItem)e.getSource()). setState(false);
            try 
            {
                undo.redo();
            } 
            catch (CannotRedoException cre) 
            {
                System.out.println("Unable to redo: " + cre);
                cre.printStackTrace();
            }
            update();
            undoAction.update();
        }

        protected void update() 
        {
            if(undo.canRedo()) 
            {
                setEnabled(true);
                putValue(Action.NAME, undo.getRedoPresentationName());
            }
            else 
            {
                setEnabled(false);
                putValue(Action.NAME, "Redo");
            }
        }
    }

    class OpenAction extends NewAction 
    {
        OpenAction() 
        {
            super(openAction);
        }

        public void actionPerformed(ActionEvent e) 
        {
            Object c = e.getSource();
            if (c instanceof JCheckBoxMenuItem)
                ((JCheckBoxMenuItem)c). setState(false);
            Frame frame = getFrame();
            JFileChooser chooser = new JFileChooser();
            int ret = chooser.showOpenDialog(frame);

            if (ret != JFileChooser.APPROVE_OPTION) 
            {
                return;
            }

            File f = chooser.getSelectedFile();
            if (f.isFile() && f.canRead()) 
            {
                Document oldDoc = getEditor().getDocument();
                if(oldDoc != null)
                    oldDoc.removeUndoableEditListener(undoHandler);
                getEditor().setDocument(new PlainDocument());
                frame.setTitle(f.getName());
                Thread loader = new FileLoader(f, editor.getDocument());
                loader.start();
            } 
            else 
            {                
                JOptionPane.showMessageDialog(getFrame(),
                        "Could not open file: " + f, "Error opening file", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    class SaveAction extends AbstractAction 
    {
        SaveAction() 
        {
            super(saveAction);
        }

        public void actionPerformed(ActionEvent e) 
        {
            Object c = e.getSource();
            if (c instanceof JCheckBoxMenuItem)
                ((JCheckBoxMenuItem)c). setState(false);
            Frame frame = getFrame();

            {
                JFileChooser chooser = new JFileChooser();
                int ret = chooser.showSaveDialog(frame);

                if (ret != JFileChooser.APPROVE_OPTION) 
                {
                    return;
                }

                File f = chooser.getSelectedFile();
                frame.setTitle(f.getName());
                Thread saver = new FileSaver(f, editor.getDocument());
                saver.start();
            }
            SaveStaus = true;        
        }
    }

    class Save_AsAction extends AbstractAction 
    {
        Save_AsAction() 
        {
            super(save_asAction);
        }

        public void actionPerformed(ActionEvent e) 
        {
            Object c = e.getSource();
            if (c instanceof JCheckBoxMenuItem)
                ((JCheckBoxMenuItem)c). setState(false);
            Frame frame = getFrame();

            {
                JFileChooser chooser = new JFileChooser();
                int ret = chooser.showSaveDialog(frame);

                if (ret != JFileChooser.APPROVE_OPTION) 
                {
                    return;
                }

                File f = chooser.getSelectedFile();
                frame.setTitle(f.getName());
                Thread saver = new FileSaver(f, editor.getDocument());
                saver.start();
            }
            SaveStaus = true;        
        }
    }

    class NewAction extends AbstractAction 
    {
        NewAction() 
        {
            super(newAction);
        }

        NewAction(String name) 
        {
            super(name);
        }

        public void actionPerformed(ActionEvent e) 
        {
            Object c = e.getSource();
            if (c instanceof JCheckBoxMenuItem)
                ((JCheckBoxMenuItem)c). setState(false);
            Document oldDoc = getEditor().getDocument();
            if(oldDoc != null)
            oldDoc.removeUndoableEditListener(undoHandler);
            getEditor().setDocument(new PlainDocument());
            getEditor().getDocument().addUndoableEditListener(undoHandler);
            resetUndoManager();
            getFrame().setTitle(resources.getString("Title"));
            revalidate();
        }
    }

    /**
     * Really lame implementation of an exit command
     */
    class ExitAction extends AbstractAction {
        ExitAction() 
        {
            super(exitAction);
        }

        public void actionPerformed(ActionEvent e) 
        {
            Object c = e.getSource();
            if (c instanceof JCheckBoxMenuItem)
                ((JCheckBoxMenuItem)c). setState(false);
            System.exit(0);
        }
    }

    /**
     * Action that brings up a JFrame with a JTree showing the structure
     * of the document.
     */

    /**
     * Thread to load a file into the text storage model
     */
    class FileLoader extends Thread 
    {
        FileLoader(File f, Document doc) 
        {
            setPriority(4);
            this.f = f;
            this.doc = doc;
        }

        public void run() 
        {
            try 
            {
            // initialize the statusbar
                status.removeAll();
                JProgressBar progress = new JProgressBar();
                progress.setMinimum(0);
                progress.setMaximum((int) f.length());
                status.add(progress);
                status.revalidate();

            // try to start reading
                Reader in = new FileReader(f);
                char[] buff = new char[4096];
                int nch;
                while ((nch = in.read(buff, 0, buff.length)) != -1) 
                {
                    doc.insertString(doc.getLength(), new String(buff, 0, nch), null);
                    progress.setValue(progress.getValue() + nch);
                }
            }
            catch (IOException e) 
            {
                final String msg = e.getMessage();
                SwingUtilities.invokeLater(new Runnable() 
                {
                    public void run() 
                    {
                        JOptionPane.showMessageDialog(getFrame(),
                                "Could not open file: " + msg,
                                "Error opening file",
                                JOptionPane.ERROR_MESSAGE);
                    }
                });
            }
            catch (BadLocationException e) 
            {
                System.err.println(e.getMessage());
            }
            doc.addUndoableEditListener(undoHandler);
            // we are done... get rid of progressbar
            status.removeAll();
            status.revalidate();

            resetUndoManager();
        }

        Document doc;
        File f;
    }

    /**
     * Thread to save a document to file
     */
    class FileSaver extends Thread 
    {
        Document doc;
        File f;

        FileSaver(File f, Document doc) 
        {
            setPriority(4);
            this.f = f;
            this.doc = doc;
        }

        public void run() 
        {
            // initialize the statusbar
            JProgressBar progress = new JProgressBar();
            progress.setMinimum(0);
            progress.setMaximum((int) doc.getLength());
            status.add(progress);
            status.revalidate();
            try 
            {
            // start writing
                Writer out = new FileWriter(f);
                Segment text = new Segment();
                text.setPartialReturn(true);
                int charsLeft = doc.getLength();
                int offset = 0;
                while (charsLeft > 0) 
                {
                    doc.getText(offset, Math.min(4096, charsLeft), text);
                    out.write(text.array, text.offset, text.count);
                    charsLeft -= text.count;
                    offset += text.count;
                    progress.setValue(offset);
                    try 
                    {
                        Thread.sleep(10);
                    } 
                    catch (InterruptedException e) 
                    {
                        e.printStackTrace();
                    }
                }
                out.flush();
                out.close();
            }
            catch (IOException e) 
            {
                final String msg = e.getMessage();
                SwingUtilities.invokeLater(new Runnable() 
                {
                    public void run() 
                    {
                        JOptionPane.showMessageDialog(getFrame(),
                                "Could not save file: " + msg, "Error saving file", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }
            catch (BadLocationException e) 
            {
                System.err.println(e.getMessage());
            }
            finally
            {
                status.remove(progress);
                status.revalidate();
            }
        }
    }
    
    class findBox extends JDialog 
    {
        public findBox() 
        {
            super((JFrame)parent1);
            Container c = new Container();
            c.setLayout(new GridLayout(2, 3, 5, 5));
            c.add(tLabel, 0); 
            c.add(tField, 1); 
            c.add(nextButton, 2);
            if (SelText.length() < 1)
                nextButton.setEnabled(false);
            c.add(jcb, 3);
            c.add(new Container(), 4);
            c.setVisible(true);
            add(c, BorderLayout.CENTER);
            
            setTitle("Find");
            setIconImage(Toolkit.getDefaultToolkit().createImage(getResource("findImage")));
            setModal(false);
            setName("findBox");
            setResizable(false);
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            Dimension d = getSize();
            d.setSize(360, 80);
            setSize(d);
            setVisible(true);
            
            //SelText = tField.getText();
            tField.selectAll();
            nextButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    MatchCase = jcb.isSelected();
                    if (SelText.length() > 0)
                        FindNext(parent1, MatchCase);
                }
            });
            
            tField.addCaretListener(new CaretListener()
            {
                public void caretUpdate(CaretEvent e)
                {
                    SelText = tField.getText();
                    if (SelText.length() == 0)
                        nextButton.setEnabled(false);
                    else
                        nextButton.setEnabled(true);
                }
            });
        }

        
        JButton nextButton = new JButton("Next");
        JTextField tField = new JTextField(SelText, 30);
        JLabel tLabel = new JLabel("Find Content: ");
        JCheckBox jcb = new JCheckBox("Match Case.", false);
    }    
    
    class replaceBox extends JDialog
    {
        public replaceBox() 
        {
            super((JFrame)parent1);
            
            Container c = new Container();
            c.setLayout(new GridLayout(3, 3, 5, 5));
            c.add(tLabel, 0);
            c.add(tField, 1);
            c.add(nextButton, 2); 
            if (SelText.length() < 1)
                nextButton.setEnabled(false);
            c.add(tLabel1); 
            c.add(tField1); 
            c.add(replaceButton); 
            c.add(jcb);
            c.add(new Container());
            c.add(replaceallButton); 
            c.setVisible(true);
            add(c, BorderLayout.CENTER);
            
            setTitle("REPLACE");
            setIconImage(Toolkit.getDefaultToolkit().createImage(getResource("replaceImage")));
            setModal(false);
            setName("replaceBox");
            setResizable(false);
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            Dimension d = getSize();
            d.setSize(360, 120);
            setSize(d);
            setVisible(true);
            
            //SelText = tField.getText();
            tField.selectAll();
            
            nextButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    MatchCase = jcb.isSelected();
                    if (SelText.length() > 0)
                        FindNext(parent1, MatchCase);
                }
            });
            
            tField.addCaretListener(new CaretListener()
            {
                public void caretUpdate(CaretEvent e)
                {
                    SelText = tField.getText();
                    if (SelText.length() == 0)
                        nextButton.setEnabled(false);
                    else
                        nextButton.setEnabled(true);
                }
            });
            
            replaceButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    MatchCase = jcb.isSelected();
                    if (SelText.length() > 0)
                    {
                        String str = editor.getSelectedText();
                        if ((str != null) && str.equals(SelText))
                            editor.replaceSelection(ReplaceText);
                        else
                        {
                            FindNext(parent1, MatchCase);
                        }
                    }
                }
            });
            
            tField1.addCaretListener(new CaretListener()
            {
                public void caretUpdate(CaretEvent e)
                {
                    ReplaceText = tField1.getText();
                }
            });
            
            replaceallButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    MatchCase = jcb.isSelected();
                    if (SelText.length() > 0)
                    {
                        replaceall(parent1, MatchCase);
                    }
                }
            });
        }

        
        JButton nextButton = new JButton("Next");
        JTextField tField = new JTextField(SelText, 30);
        JLabel tLabel = new JLabel("Find Content: ");
        JButton replaceButton = new JButton("Replace");
        JTextField tField1 = new JTextField(ReplaceText, 30);
        JLabel tLabel1 = new JLabel("Replace to  : ");
        JButton replaceallButton = new JButton("Replace All");
        JCheckBox jcb = new JCheckBox("Match Case.", false);
    }    
}