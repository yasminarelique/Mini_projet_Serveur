/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ISIS.adventureISIServeur.Classes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 *
 * @author Yasmina
 */
public class Services {

    public World getWorld(String username) throws JAXBException {
        return readWorldFromXml(username);
    }

    public World readWorldFromXml(String username) throws JAXBException {
        String filename = username + "_world.xml";

        try {
            File temp = new File(filename);
            JAXBContext cont = JAXBContext.newInstance(World.class);
            Unmarshaller u = cont.createUnmarshaller();
            World world = (World) u.unmarshal(temp);
            return world;
        } catch (Exception e) {
            InputStream input = getClass().getClassLoader().getResourceAsStream("world.xml");
            //Unmarwhaller
            JAXBContext cont = JAXBContext.newInstance(World.class);
            Unmarshaller u = cont.createUnmarshaller();
            World world = (World) u.unmarshal(input);
            return world;
        }

    }

    void saveWorldToXml(World world, String username) throws FileNotFoundException, JAXBException {
        OutputStream output = new FileOutputStream(username + "world.xml");
        JAXBContext cont = JAXBContext.newInstance(World.class);
        Marshaller m = cont.createMarshaller();
        m.marshal(world, output);
    }

    // prend en paramètre le pseudo du joueur et le produit
    // sur lequel une action a eu lieu (lancement manuel de production ou 
    // achat d’une certaine quantité de produit)
    // renvoie false si l’action n’a pas pu être traitée  
    public Boolean updateProduct(String username, ProductType newproduct) throws FileNotFoundException, JAXBException {
    // aller chercher le monde qui correspond au joueur
        World world = getWorld(username);

    // trouver dans ce monde, le produit équivalent à celui passé
    // en paramètre
        ProductType product = findProductById(world, newproduct.getId());
        if (product == null) {
            return false;
        }
    // calculer la variation de quantité. Si elle est positive c'est
    // que le joueur a acheté une certaine quantité de ce produit
    // sinon c’est qu’il s’agit d’un lancement de production.
        int qtchange = newproduct.getQuantite() - product.getQuantite();
        if (qtchange > 0) {
        // soustraire de l'argent du joueur le cout de la quantité
    // achetée et mettre à jour la quantité de product 
            double prix = product.getCout();
            double q = product.getCroissance();
            double prixSomme = prix*((1-Math.pow(q,qtchange))/(1-q));
            double argent = world.getMoney();
            double argentRestant = argent-prixSomme;
            product.setQuantite(newproduct.getQuantite());
            world.setMoney(argentRestant);
            
            
            
    // soustraire de l'argent du joueur le cout de la quantité
    // achetée et mettre à jour la quantité de product 
        } else {
            product.setTimeleft(product.getVitesse());
    // initialiser product.timeleft à product.vitesse
    // pour lancer la production
        }
    // sauvegarder les changements du monde
        saveWorldToXml(world, username);
        return true;
    }

    // prend en paramètre le pseudo du joueur et le manager acheté.
// renvoie false si l’action n’a pas pu être traitée  
    public Boolean updateManager(String username, PallierType newmanager) throws JAXBException, FileNotFoundException {
// aller chercher le monde qui correspond au joueur
        World world = getWorld(username);
// trouver dans ce monde, le manager équivalent à celui passé
// en paramètre
        PallierType manager = findManagerByName(world, newmanager.getName());
        if (manager == null) {
            return false;
        }
        // débloquer ce manager
        manager.setUnlocked(true);
// trouver le produit correspondant au manager
        ProductType product = findProductById(world, manager.getIdcible());
        if (product == null) {
            return false;
        }


// soustraire de l'argent du joueur le cout du manager
            double prixm = manager.getSeuil();
            double argent = world.getMoney();
            double argentRestant = argent-prixm;
            world.setMoney(argentRestant);
// sauvegarder les changements au monde
        saveWorldToXml(world, username);
        return true;
    }

    private ProductType findProductById(World world, int id) {
        ProductType produit = null;
        List<ProductType> products = world.getProducts().getProduct();
        for (ProductType p: products){
            if(p.getId()==id){
                produit=p;
            }
        }      
        return produit;
    }

    private PallierType findManagerByName(World world, String name) {
    PallierType manager = null;
        List<PallierType> palliers = world.getManagers().getPallier();
        for (PallierType m: palliers){
            if(m.getName().equals(name)){
                manager=m;
            }
        }      
        return manager;
    }

}
