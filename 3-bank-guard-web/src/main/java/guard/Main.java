/* Fraud Management System (Part 2, web version) - B NIKITHA, 25BCE2517 */
package guard;

import com.github.mvysny.vaadinboot.VaadinBoot;

/** Starts the web version on an embedded Jetty server: open http://localhost:8080/fraud */
public final class Main {
    public static void main(String[] args) throws Exception {
        System.setProperty("vaadin.productionMode", "true");
        new VaadinBoot().run();
    }
}
