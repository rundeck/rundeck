import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.ContextHandlerCollection
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.server.Handler



eventConfigureJetty = { Server server ->
    try {
        WebAppContext ctx=server.handler
        ctx.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
    }
    catch (e) {
        e.printStackTrace()
    }
}
