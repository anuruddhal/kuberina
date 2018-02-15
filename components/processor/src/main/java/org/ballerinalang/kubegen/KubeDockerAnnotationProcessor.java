package org.ballerinalang.kubegen;

import org.ballerinalang.annotation.AbstractAnnotationProcessor;
import org.ballerinalang.annotation.AnnotationType;
import org.ballerinalang.annotation.SupportedAnnotations;
import org.ballerinalang.model.tree.AnnotationAttachmentNode;
import org.ballerinalang.model.tree.FunctionNode;
import org.ballerinalang.model.tree.ServiceNode;
import org.ballerinalang.util.diagnostic.Diagnostic;
import org.ballerinalang.util.diagnostic.DiagnosticLog;

import java.io.PrintStream;
import java.util.List;

/**
 * This class validates annotations attached to Ballerina service and resource nodes.
 *
 * @since 1.0
 */
@SupportedAnnotations(
        value = {@AnnotationType(packageName = "ballerina.kubernetes", name = "deployment"),
                @AnnotationType(packageName = "ballerina.net.http", name = "configuration"),
                @AnnotationType(packageName = "ballerina.docker", name = "configuration")
        }
)
public class KubeDockerAnnotationProcessor extends AbstractAnnotationProcessor {
    DiagnosticLog dlog;
    PrintStream out = System.out;

    public void init(DiagnosticLog diagnosticLog) {
        this.dlog = diagnosticLog;
    }

    @Override
    public void process(ServiceNode serviceNode, List<AnnotationAttachmentNode> annotations) {
        out.println("kuberina service node: " + serviceNode.getName().getValue());
        for (AnnotationAttachmentNode attachmentNode : annotations) {
            out.println(attachmentNode.getPackageAlias().getValue() + ":" + attachmentNode.getAnnotationName()
                    .getValue());
        }

        // This is how you can report compilation errors, warnings, and messages.
        dlog.logDiagnostic(Diagnostic.Kind.WARNING, serviceNode.getPosition(), "Dummy kuberina warning message");
    }

    @Override
    public void process(FunctionNode functionNode, List<AnnotationAttachmentNode> annotations) {
        out.println("function node: " + functionNode.getName().getValue());
        for (AnnotationAttachmentNode attachmentNode : annotations) {
            out.println(attachmentNode.getAnnotationName().getValue());
        }
    }

}
