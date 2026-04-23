package tn.esprit.tools;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;

public final class SelfSignedCert {

    public static final char[] PASSWORD = "skillora-face".toCharArray();

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private SelfSignedCert() {}

    public static KeyStore loadOrCreate(String lanIp) throws Exception {
        File file = storeFile();
        KeyStore ks = KeyStore.getInstance("PKCS12");
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                ks.load(fis, PASSWORD);
                return ks;
            } catch (Exception ignored) { /* regenerate */ }
        }
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048, new SecureRandom());
        KeyPair kp = kpg.generateKeyPair();

        X500Name issuer = new X500Name("CN=SkillORA Face ID, O=SkillORA, C=TN");
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = new Date(System.currentTimeMillis() - 86400000L);
        Date notAfter = new Date(System.currentTimeMillis() + 10L * 365 * 86400000L);

        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuer, serial, notBefore, notAfter, issuer, kp.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        GeneralName[] names = new GeneralName[] {
                new GeneralName(GeneralName.iPAddress, lanIp),
                new GeneralName(GeneralName.dNSName, "localhost"),
                new GeneralName(GeneralName.iPAddress, "127.0.0.1")
        };
        builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(names));

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
                .setProvider("BC").build(kp.getPrivate());
        X509CertificateHolder holder = builder.build(signer);
        X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);

        ks.load(null, PASSWORD);
        ks.setKeyEntry("face-id", kp.getPrivate(), PASSWORD, new X509Certificate[]{cert});
        file.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            ks.store(fos, PASSWORD);
        }
        return ks;
    }

    private static File storeFile() {
        String appdata = System.getenv("APPDATA");
        String base = appdata != null ? appdata : System.getProperty("user.home");
        return new File(base, "SkillORA/face-cert.p12");
    }
}
