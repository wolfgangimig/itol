package com.wilutions.itol.experimental;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.web.HTMLEditor;
import javafx.stage.Stage;

public class HTMLEditorModifier extends Application {

      public static void main(String[] args) { launch(args); }
      @Override public void start(Stage stage) {
        final HTMLEditor htmlEditor = new HTMLEditor();
        stage.setScene(new Scene(htmlEditor));
        
        stage.setWidth(300);
        stage.setHeight(200);
        stage.show();
        
        for (Node candidate: (htmlEditor.lookupAll("ComboBox"))) 
        {
          if (candidate instanceof ComboBox)
          {
              ComboBox cb = (ComboBox) candidate;
              System.out.println("cb=" + cb);
          }
        }

        
        addCustomToolBarTo(htmlEditor);

        printChildren(htmlEditor, 20);
        
        {
        	for (Node n : htmlEditor.lookupAll(".html-editor-outdent")) {
        		System.out.println("n=" + n);
        	}
        }

//        moveFromTo(htmlEditor, "PopupButton", 0, "ToolBar", 2);
//        moveFromTo(htmlEditor, "PopupButton", 1, "ToolBar", 2);

//        moveFromTo(htmlEditor, "Separator", 4, "ToolBar", 2);
//        moveFromTo(htmlEditor, "ComboBox", 2, "ToolBar", 2);
//        moveFromTo(htmlEditor, "Separator", 5, "ToolBar", 2);
//
//        moveFromTo(htmlEditor, "ToggleButton", 6, "ToolBar", 2);
//        moveFromTo(htmlEditor, "ToggleButton", 7, "ToolBar", 2);
//        moveFromTo(htmlEditor, "ToggleButton", 8, "ToolBar", 2);

//        removeFrom(htmlEditor, "ToolBar", 1);
//        removeFrom(htmlEditor, "ToolBar", 0);

        //printChildren(htmlEditor, 20);

      }

      public void moveFromTo(HTMLEditor he, String t, int c, String t2, int c2)
      {
          Node nCb = new Button(); //just has to be sth.  

          //Copy From:
          int i = 0;
          switch(t)
          {
              case "PopupButton":
                  for (Node candidate: (he.lookupAll("PopupButton"))) 
                  {
                    if (candidate instanceof Node)
                    {
                    	Node cb = (Node) candidate;
                        if (i == c)
                        {
                            nCb = cb;
                            break;
                        }
                    }
                    i++;
                  }
                  break;  
              case "Separator":
                  for (Node candidate: (he.lookupAll("Separator"))) 
                  {
                    if (candidate instanceof Separator)
                    {
                        Separator cb = (Separator) candidate;
                        if (i == c)
                        {
                            nCb = cb;
                            break;
                        }
                    }
                    i++;
                  }
                  break;
              case "ComboBox":
                  for (Node candidate: (he.lookupAll("ComboBox"))) 
                  {
                    if (candidate instanceof ComboBox)
                    {
                        ComboBox cb = (ComboBox) candidate;
                        if (i == c)
                        {
                            nCb = cb;
                            break;
                        }
                    }
                    i++;
                  }
                  break;    
              case "ToggleButton":
                  for (Node candidate: (he.lookupAll("ToggleButton"))) 
                  {
                    if (candidate instanceof ToggleButton)
                    {
                        ToggleButton cb = (ToggleButton) candidate;
                        if (i == c)
                        {
                            nCb = cb;
                            break;
                        }
                    }
                    i++;
                  }
                  break;    
          }
          //Copy To:
          i = 0;
          switch(t2)
          {
              case "ToolBar":
                  for (Node candidate: (he.lookupAll("ToolBar"))) 
                  {
                    if (candidate instanceof ToolBar)
                    {
                        ToolBar cb2 = (ToolBar) candidate;
                        if (i == c2)
                        {
                            cb2.getItems().add(nCb);
                            break;
                        }
                    }
                    i++;
                  }
                  break;    
          }
      }

      public void removeFrom(HTMLEditor he, String t, int c)
      {
          int i = 0;

          switch(t)
          {
          case "ToolBar":
              for (Node candidate: (he.lookupAll("ToolBar"))) 
              {
                if (candidate instanceof ToolBar)
                {
                    ToolBar cb = (ToolBar) candidate;
                    if (i == c)
                    {
                        Node nCb = cb;
                        ((Pane) nCb.getParent()).getChildren().remove(nCb);
                        break;
                    }
                }
                i++;
              }
              break;
          case "PopupButton":
              for (Node candidate: (he.lookupAll("PopupButton"))) 
              {
                    if (i == c)
                    {
                        Node nCb = candidate;
                        nCb.setVisible(false); nCb.setManaged(false);
                        break;
                    }
                    i++;
              }
              break;
          case "ToggleButton":
              for (Node candidate: (he.lookupAll("ToggleButton"))) 
              {
                if (candidate instanceof ToggleButton)
                {
                    ToggleButton cb = (ToggleButton) candidate;
                    if (i == c)
                    {
                        Node nCb = cb;
                        nCb.setVisible(false); nCb.setManaged(false);
                        break;
                    }
                }
                i++;
              }
              break;
          case "Separator":
              for (Node candidate: (he.lookupAll("Separator"))) 
              {
                if (candidate instanceof Separator)
                {
                    Separator cb = (Separator) candidate;
                    if (i == c)
                    {
                        Node nCb = cb;
                        nCb.setVisible(false); nCb.setManaged(false);
                        break;
                    }
                }
                i++;
              }
              break;
          case "Button":
              for (Node candidate: (he.lookupAll("Button"))) 
              {
                if (candidate instanceof Button)
                {
                    Button cb = (Button) candidate;
                    if (i == c)
                    {
                        Node nCb = cb;
                        nCb.setVisible(false); nCb.setManaged(false);
                        break;
                    }
                }
                i++;
              }
              break;
          case "ComboBox":
              for (Node candidate: (he.lookupAll("ComboBox"))) 
              {
                if (candidate instanceof ComboBox)
                {
                    ComboBox cb = (ComboBox) candidate;
                    if (i == c)
                    {
                        Node nCb = cb;
                        nCb.setVisible(false); nCb.setManaged(false);
                        break;
                    }
                }
                i++;
              }
              break;
          }   
      }

      public void printChildren(HTMLEditor he, int MAXDEPTH)
      {
          System.out.println("Print Children ==========>>>>");
          String[] hieraArray = new String[MAXDEPTH]; 
          int maxDepth = 0;
          int lastDepth = 0;
          Node parent;

            /* List all elements of the HTMLeditor */
            for (Node element: (he.lookupAll("*")))
            {
                parent = element.getParent();
                if (maxDepth == 0) 
                {
                    hieraArray[0] = element.getClass().getSimpleName().toString();
                    System.out.print(hieraArray[0]);
                    System.out.println("");
                    maxDepth = 1;                   
                }
                else 
                {
                    int i = 0, i2 = 0;
                    boolean found = false;
                    for(i=maxDepth; i>=0; i--)
                    {
                        if (hieraArray[i] == null || parent.getClass().getSimpleName() == null) continue;
                        if (hieraArray[i].equals(parent.getClass().getSimpleName()))
                        {
                            for (i2 = 0; i2 <= i; i2++)
                            {
                                System.out.print("|");
                            }   
                            if ((Math.abs(lastDepth-i2)) > 2) System.out.print("->" + element.getClass().getSimpleName() + " {p: " + parent.getClass().getSimpleName() + "}");
                            else {
                            	System.out.print("->" + element + ", id=" + element.getId());
                            }
                            //if (element.getClass().getSimpleName().equals("PopupButton"))  System.out.print(" ??: " + element + " ::: " + element.getClass());
                            lastDepth = i2;

                            hieraArray[(i+1)] = element.getClass().getSimpleName();
                            if (maxDepth < (i+1)) maxDepth = (i+1);
                            found = true;
                            System.out.println("");
                            break;
                        }
                    }
                    if (found == false) 
                    {
                        hieraArray[(i+1)] = parent.getClass().getSimpleName();
                        if (maxDepth < (i+1)) maxDepth = (i+1);
                    }
                    if ((maxDepth+1) >= MAXDEPTH) 
                    {
                        System.out.println("MAXDEPTH reached! increase ArraySize!");
                        return;
                    }
                }
            }


      }

      public ToolBar addCustomToolBarTo(HTMLEditor he)
      {
          /* Thers one GridPane to the HTMLEditor where we add the ToolBar */
          ToolBar customTB = new ToolBar();
          for (Node candidate: (he.lookupAll("GridPane"))) 
          {
            if (candidate instanceof GridPane)
            {
                ((GridPane) candidate).getChildren().add(customTB);
                break;
            }   
          }
          return customTB;
      }       
}
