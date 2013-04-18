/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.mihosoft.vrl.workflow.fx;

import eu.mihosoft.vrl.workflow.VFlow;
import eu.mihosoft.vrl.workflow.VNode;
import eu.mihosoft.vrl.workflow.FlowNodeSkin;
import java.util.HashMap;
import java.util.Map;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import jfxtras.labs.scene.control.window.Window;
import jfxtras.labs.util.NodeUtil;

/**
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class FXFlowNodeSkin
        implements FXSkin<VNode, Window>, FlowNodeSkin<VNode> {

    private ObjectProperty<VNode> modelProperty = new SimpleObjectProperty<>();
//    private ObjectProperty<Flow> flowProperty = new SimpleObjectProperty<>();
    private FlowNodeWindow node;
    private ObjectProperty<Parent> parentProperty = new SimpleObjectProperty<>();
    private ChangeListener<String> modelTitleListener;
    private ChangeListener<Number> modelXListener;
    private ChangeListener<Number> modelYListener;
    private ChangeListener<Number> modelWidthListener;
    private ChangeListener<Number> modelHeightListener;
    private ChangeListener<Number> nodeXListener;
    private ChangeListener<Number> nodeYListener;
    private ChangeListener<Number> nodeWidthListener;
    private ChangeListener<Number> nodeHeightListener;
//    private Node output;
    private FXNewConnectionSkin newConnectionSkin;
    private boolean removeSkinOnly = false;
    private VFlow controller;
    private Map<String, Node> outputs = new HashMap<>();

    public FXFlowNodeSkin(Parent parent, VNode model, VFlow controller) {

        setParent(parent);
        setModel(model);

        this.controller = controller;

        init();
    }

    private void init() {
        node = new FlowNodeWindow(this);

        node.setTitle(getModel().getTitle());
        node.setLayoutX(getModel().getX());
        node.setLayoutY(getModel().getY());
        node.setPrefWidth(getModel().getWidth());
        node.setPrefHeight(getModel().getHeight());

        registerListeners(getModel());

        modelProperty.addListener(new ChangeListener<VNode>() {
            @Override
            public void changed(ObservableValue<? extends VNode> ov, VNode oldVal, VNode newVal) {

                removeListeners(oldVal);
                registerListeners(newVal);
            }
        });

        for (String type : getModel().getOutputTypes()) {
            addOutputConnector(type);
        }

        getModel().getOutputTypes().addListener(new ListChangeListener<String>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends String> change) {
                while (change.next()) {
                    if (change.wasPermutated()) {
                        for (int i = change.getFrom(); i < change.getTo(); ++i) {
                            //permutate
                        }
                    } else if (change.wasUpdated()) {
                        //update item
                    } else if (change.wasRemoved()) {
                        // removed
                        for (String type : change.getRemoved()) {
                            removeOutputConnector(type);
                        }
                    } else if (change.wasAdded()) {
                        // added
                        for (String type : change.getAddedSubList()) {
                            addOutputConnector(type);
                        }
                    }

                } // end while change.next()
            }
        });

    }

    private void addOutputConnector(final String type) {
        DoubleBinding startXBinding = new DoubleBinding() {
            {
                super.bind(node.layoutXProperty(), node.widthProperty());
            }

            @Override
            protected double computeValue() {
                return node.getLayoutX() + node.getWidth();
            }
        };

        DoubleBinding startYBinding = new DoubleBinding() {
            {
                super.bind(node.layoutYProperty(), node.heightProperty());
            }

            @Override
            protected double computeValue() {
                return node.getLayoutY() + node.getHeight() / 2;
            }
        };

        Circle circle = new Circle(20);

        if (type.equals("control")) {
            circle.setFill(new Color(1.0, 1.0, 0.0, 0.75));
            circle.setStroke(new Color(120 / 255.0, 140 / 255.0, 1, 0.42));
        } else if (type.equals("data")) {
            circle.setFill(new Color(0.1, 0.1, 0.1, 0.5));
            circle.setStroke(new Color(120 / 255.0, 140 / 255.0, 1, 0.42));
        } else if (type.equals("event")) {
            circle.setFill(new Color(255.0 / 255.0, 100.0 / 255.0, 1, 0.5));
            circle.setStroke(new Color(120 / 255.0, 140 / 255.0, 1, 0.42));
        }

        circle.setStrokeWidth(3);

        final Node output = circle;

        output.layoutXProperty().bind(startXBinding);
        output.layoutYProperty().bind(startYBinding);

        NodeUtil.addToParent(getParent(), output);

        output.onMouseEnteredProperty().set(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {
                output.toFront();
            }
        });

        output.onMousePressedProperty().set(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {

//                if (getModel().getFlow().getConnections("control").
//                        isOutputConnected(getModel().getId())) {
//                    return;
//                }

                newConnectionSkin =
                        new FXNewConnectionSkin(
                        getParent(), getModel(), getController(), type);

                newConnectionSkin.add();

                t.consume();
                MouseEvent.fireEvent(newConnectionSkin.getReceiverConnector(), t);
            }
        });

        output.onMouseDraggedProperty().set(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {
                t.consume();
                MouseEvent.fireEvent(newConnectionSkin.getReceiverConnector(), t);

            }
        });

        output.onMouseReleasedProperty().set(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {
                t.consume();
                try {
                    MouseEvent.fireEvent(newConnectionSkin.getReceiverConnector(), t);
                } catch (Exception ex) {
                    // TODO exception is not critical here (node already removed)
                }
                output.toBack();
            }
        });

        output.onMouseExitedProperty().set(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {
                output.toBack();
            }
        });

        outputs.put(type, output);
    }

    private void removeOutputConnector(String type) {
        Node output = outputs.get(type);
        if (output != null) {
            NodeUtil.removeFromParent(output);
        }
    }

    @Override
    public Window getNode() {
        return node;
    }

    @Override
    public Parent getContentNode() {
        return node.getWorkflowContentPane();
    }

    @Override
    public void remove() {
        removeSkinOnly = true;
        for (String type : outputs.keySet()) {
            removeOutputConnector(type);
        }
        NodeUtil.removeFromParent(node);
    }

    @Override
    public final void setModel(VNode model) {
        modelProperty.set(model);
    }

    @Override
    public final VNode getModel() {
        return modelProperty.get();
    }

    @Override
    public final ObjectProperty<VNode> modelProperty() {
        return modelProperty;
    }

    final void setParent(Parent parent) {
        parentProperty.set(parent);
    }

    Parent getParent() {
        return parentProperty.get();
    }

    ObjectProperty<Parent> parentProperty() {
        return parentProperty;
    }

    @Override
    public void add() {
        NodeUtil.addToParent(getParent(), node);
    }

    private void removeListeners(VNode flowNode) {
        flowNode.titleProperty().removeListener(modelTitleListener);
        flowNode.xProperty().removeListener(modelXListener);
        flowNode.yProperty().removeListener(modelYListener);
        flowNode.widthProperty().removeListener(modelWidthListener);
        flowNode.heightProperty().removeListener(modelHeightListener);

        node.layoutXProperty().removeListener(nodeXListener);
        node.layoutYProperty().removeListener(nodeXListener);
        node.prefWidthProperty().removeListener(nodeWidthListener);
        node.prefHeightProperty().removeListener(nodeHeightListener);
    }

    private void initListeners() {
        modelTitleListener = new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> ov, String oldVal, String newVal) {
                node.setTitle(newVal);
            }
        };

        modelXListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number oldVal, Number newVal) {
                node.setLayoutX((double) newVal);
            }
        };

        modelYListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number oldVal, Number newVal) {
                node.setLayoutY((double) newVal);
            }
        };

        modelWidthListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number oldVal, Number newVal) {
                node.setPrefWidth((double) newVal);
            }
        };

        modelHeightListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number oldVal, Number newVal) {
                node.setPrefHeight((double) newVal);
            }
        };

        nodeXListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number oldVal, Number newVal) {
                getModel().setX((double) newVal);
            }
        };

        nodeYListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number oldVal, Number newVal) {
                getModel().setY((double) newVal);
            }
        };

        nodeWidthListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number oldVal, Number newVal) {
                getModel().setWidth((double) newVal);
            }
        };

        nodeHeightListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number oldVal, Number newVal) {
                getModel().setHeight((double) newVal);
            }
        };

        node.onCloseActionProperty().set(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                if (!removeSkinOnly) {
                    modelProperty().get().getFlow().remove(modelProperty().get());
                }
            }
        });
    }

    private void registerListeners(VNode flowNode) {

        initListeners();

        flowNode.titleProperty().addListener(modelTitleListener);
        flowNode.xProperty().addListener(modelXListener);
        flowNode.yProperty().addListener(modelYListener);
        flowNode.widthProperty().addListener(modelWidthListener);
        flowNode.heightProperty().addListener(modelHeightListener);

        node.layoutXProperty().addListener(nodeXListener);
        node.layoutYProperty().addListener(nodeYListener);
        node.prefWidthProperty().addListener(nodeWidthListener);
        node.prefHeightProperty().addListener(nodeHeightListener);

    }

    /**
     * @return the controller
     */
    @Override
    public VFlow getController() {
        return controller;
    }

    /**
     * @param controller the controller to set
     */
    @Override
    public void setController(VFlow controller) {
        this.controller = controller;
    }
}
