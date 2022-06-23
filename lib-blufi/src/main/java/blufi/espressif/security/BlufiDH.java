package blufi.espressif.security;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;

public class BlufiDH {
    private final BigInteger mP;
    private final BigInteger mG;

    private final DHPrivateKey mPrivateKey;
    private final DHPublicKey mPublicKey;

    private byte[] mSecretKey;

    public BlufiDH(BigInteger p, BigInteger g, int length) {
        mP = p;
        mG = g;
        Key[] keys = generateKeys(p, g, length);
        assert keys != null;
        mPrivateKey = (DHPrivateKey) keys[0];
        mPublicKey = (DHPublicKey) keys[1];
    }

    public BigInteger getP() {
        return mP;
    }

    public BigInteger getG() {
        return mG;
    }

    public DHPrivateKey getPrivateKey() {
        return mPrivateKey;
    }

    public DHPublicKey getPublicKey() {
        return mPublicKey;
    }

    public byte[] getSecretKey() {
        return mSecretKey;
    }

    public void generateSecretKey(BigInteger y) {
        try {
            DHPublicKeySpec pbks = new DHPublicKeySpec(y, mP, mG);
            KeyFactory keyFact = KeyFactory.getInstance("DH");
            PublicKey publicKey = keyFact.generatePublic(pbks);

            // Prepare to generate the secret key with the private key and public key of the other party
            KeyAgreement ka = KeyAgreement.getInstance("DH");
            ka.init(mPrivateKey);
            ka.doPhase(publicKey, true);

            // Generate the secret key
            mSecretKey = ka.generateSecret();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    private static Key[] generateKeys(BigInteger p, BigInteger g, int length) {
        try {
            // Use the values to generate a key pair
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
            DHParameterSpec dhSpec = new DHParameterSpec(p, g, length);
            keyGen.initialize(dhSpec);
            KeyPair keypair = keyGen.generateKeyPair();

            // Get the generated public and private keys
            Key[] result = new Key[2];
            result[0] = keypair.getPrivate();
            result[1] = keypair.getPublic();

            return result;
        } catch (NoSuchAlgorithmException
                | InvalidAlgorithmParameterException
                | ClassCastException e) {
            e.printStackTrace();

            return null;
        }
    }
}
