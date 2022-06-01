package org.rundeck.plugin.scm.git.ssh

import org.eclipse.jgit.errors.UnsupportedCredentialItem
import org.eclipse.jgit.transport.CredentialItem
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.URIish

class SshjCredentialsProvider extends CredentialsProvider{
    @Override
    boolean isInteractive() {
        return false
    }

    @Override
    boolean supports(CredentialItem... items) {
        return false
    }

    @Override
    boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
        for (CredentialItem item : items) {
            if (item instanceof CredentialItem.InformationalMessage) {
                continue;
            }
            if (item instanceof CredentialItem.YesNoType) {
                // Set this according to your requirement
                ((CredentialItem.YesNoType) item).setValue(true);
            } else {
                return false;
            }
        }
        return true;
    }
}
