package org.ballerinalang.artifactgen;

import org.ballerinalang.util.codegen.AnnAttachmentInfo;
import org.ballerinalang.util.codegen.PackageInfo;
import org.ballerinalang.util.codegen.ProgramFile;
import org.ballerinalang.util.codegen.ProgramFileReader;
import org.ballerinalang.util.codegen.ServiceInfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Main class for demo purposes.
 */
public class Main {
    private static final PrintStream out = System.out;

    public static void main(String[] args) {

       // String filePath = "/Users/anuruddha/Desktop/workspace/hello/sample/hello-world.balx";
        String filePath = args[0];
        try {
            byte[] bFile = Files.readAllBytes(Paths.get(filePath));
            ProgramFileReader reader = new ProgramFileReader();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bFile);
            ProgramFile programFile = reader.readProgram(byteArrayInputStream);
            PackageInfo packageInfos[] = programFile.getPackageInfoEntries();

            for (int i = 0; i < packageInfos.length; i++) {
                PackageInfo packageInfo = packageInfos[i];
                ServiceInfo serviceInfos[] = packageInfo.getServiceInfoEntries();
                for (int j = 0; j < serviceInfos.length; j++) {
                    AnnAttachmentInfo annAttachmentInfo = serviceInfos[j].getAnnotationAttachmentInfo("ballerina" +
                                    ".kubernetes",
                            "deployment");
                    out.println(annAttachmentInfo.getName());
                }
            }
        } catch (IOException e) {
            out.println(e.getMessage());
        }


    }
}
