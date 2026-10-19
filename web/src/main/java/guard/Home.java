/* Fraud Management System (Part 2, web version) - B NIKITHA, 25BCE2517 */
package guard;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

/** http://localhost:8080/ simply opens the Bank Guard page. */
@Route("")
public class Home extends Div implements BeforeEnterObserver {
    @Override
    public void beforeEnter(BeforeEnterEvent e) { e.forwardTo(GuardView.class); }
}
