package a3;

import net.java.games.input.Event;
import tage.input.action.AbstractInputAction;

public class LeftStickPressAction extends AbstractInputAction {

    @Override
    public void performAction(float time, Event evt) {
        switch (evt.getComponent().getIdentifier().getName()) {
            case "O":
                MyGame.getGameInstance().getState().target();
                return;
        }
    }
}