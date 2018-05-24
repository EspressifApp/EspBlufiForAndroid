/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package libs.espressif.security;

import java.math.BigInteger;
import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;

import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;

public class EspDH {
    private final int mLength;
    private BigInteger mP;
    private BigInteger mG;

    private DHPrivateKey mPrivateKey;
    private DHPublicKey mPublicKey;

    private byte[] mSecretKey;

    public EspDH(BigInteger p, BigInteger g, int length) {
        mP = p;
        mG = g;
        mLength = length;
        generateKeys();
    }

    private BigInteger[] generatePG() {
        AlgorithmParameterGenerator paramGen = null;
        try {
            paramGen = AlgorithmParameterGenerator.getInstance("DH");
            paramGen.init(mLength, new SecureRandom());
            AlgorithmParameters params = paramGen.generateParameters();
            DHParameterSpec dhSpec = params.getParameterSpec(DHParameterSpec.class);
            BigInteger pv = dhSpec.getP();
            BigInteger gv = dhSpec.getG();

            return new BigInteger[]{pv, gv};
        } catch (NoSuchAlgorithmException | InvalidParameterSpecException e) {
            e.printStackTrace();
        }

        return null;
    }

    private boolean generateKeys() {
        try {
            // Use the values to generate a key pair
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
            DHParameterSpec dhSpec = new DHParameterSpec(mP, mG, mLength);
            keyGen.initialize(dhSpec);
            KeyPair keypair = keyGen.generateKeyPair();

            // Get the generated public and private keys
            mPrivateKey = (DHPrivateKey) keypair.getPrivate();
            mPublicKey = (DHPublicKey) keypair.getPublic();

            return true;
        } catch (NoSuchAlgorithmException
                | InvalidAlgorithmParameterException
                | ClassCastException e) {
            e.printStackTrace();

            return false;
        }
    }

    public BigInteger getP() {
        return mP;
    }

    public BigInteger getG() {
        return mG;
    }

    public DHPrivateKey getPriveteKey() {
        return mPrivateKey;
    }

    public DHPublicKey getPublicKey() {
        return mPublicKey;
    }

    public byte[] getSecretKey() {
        return mSecretKey;
    }

    public void generateSecretKey(BigInteger y) throws InvalidKeySpecException {
        try {
            DHPublicKeySpec ks = new DHPublicKeySpec(y, mP, mG);
            KeyFactory keyFact = KeyFactory.getInstance("DH");
            PublicKey publicKey = keyFact.generatePublic(ks);

            // Prepare to generate the secret key with the private key and public key of the other party
            KeyAgreement ka = KeyAgreement.getInstance("DH");
            ka.init(mPrivateKey);
            ka.doPhase(publicKey, true);

            // Generate the secret key
            mSecretKey = ka.generateSecret();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }
}
