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
    InputStream input = getClass().getClassLoader().getResourceAsStream("world.xml");
    public World getWorld(String username) throws JAXBException, FileNotFoundException {
        //recupere date courante
        //regarde le monde stocké
        //comparer la date de la dernière mise à jour
        //lastupdate si différent on save
        World world = readWorldFromXml(username);
        long timeCurrent = System.currentTimeMillis();
        long dateDerniere = world.getLastupdate();
        if(dateDerniere==timeCurrent){
             return world; 
        }
        majScore(world);
        world.setLastupdate(System.currentTimeMillis());
        
        saveWorldToXml(world,username);
        return world; 
        
        

       
    }
    
    public void deleteWorld(String username)throws JAXBException, FileNotFoundException {
         World monde = readWorldFromXml(username);
         double angesActifs=monde.getActiveangels();
         double nombreAngesTotal = monde.getTotalangels();
         double angesAjouté = nombreAngesGagne(monde);
         
         angesActifs+=angesActifs+angesAjouté;
         nombreAngesTotal+=nombreAngesTotal+angesAjouté;
         
         double score = monde.getScore();
         
         JAXBContext cont = JAXBContext.newInstance(World.class);
            Unmarshaller u = cont.createUnmarshaller();
            World world = (World) u.unmarshal(input);
            
             world.setTotalangels(nombreAngesTotal);
             world.setActiveangels(angesActifs);
             world.setScore(score);
             saveWorldToXml(world,username);
    }
    
    public double nombreAngesGagne(World world)throws JAXBException {
        double totalAnges = world.getTotalangels();
        double nombreAngesGagnes = Math.round(150 * Math.sqrt(world.getScore()/Math.pow(10, 15))) - totalAnges;
        return nombreAngesGagnes;
        
        
    }

 public void majScore(World world){

        List<ProductType> products = world.getProducts().getProduct();
        long timeCurrent = System.currentTimeMillis();
        long dateDerniere = world.getLastupdate();
        long delta = timeCurrent-dateDerniere;
        
        int angelBonus = world.getAngelbonus();
        
        for (ProductType p : products) {
            if(p.isManagerUnlocked())  {
                int tempsProduit=p.getVitesse();
                int nbreProduit= (int) (delta/tempsProduit);
                long tempsRestant=p.getVitesse()-(delta%tempsProduit);
                p.setTimeleft(tempsRestant);               
                double argent = p.getRevenu()*nbreProduit*(1+world.getActiveangels()*angelBonus/100);
                world.setMoney(world.getMoney()+argent);
                world.setScore(world.getScore()+argent);
            }
            else{
                if(p.getTimeleft()!=0){
                    if(p.getTimeleft()<delta){
                        double score = world.getScore();
                        world.setScore(world.getScore()+p.revenu);
                        double money = world.getMoney();
                        world.setMoney(world.getMoney()+p.revenu);
                    }
                    else{
                        long timeleft = p.getTimeleft();
                        p.setTimeleft(timeleft-delta);
                    }
                }
                
            }
        }
       
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
            double prixSomme = prix * ((1 - Math.pow(q, qtchange)) / (1 - q));
            double argent = world.getMoney();
            double argentRestant = argent - prixSomme;
            product.setQuantite(newproduct.getQuantite());
            world.setMoney(argentRestant);

            // soustraire de l'argent du joueur le cout de la quantité
            // achetée et mettre à jour la quantité de product 
        } else {
            product.setTimeleft(product.getVitesse());
            // initialiser product.timeleft à product.vitesse
            // pour lancer la production
        }
        List<PallierType> pallier = product.getPalliers().getPallier();
        
        for (PallierType a : pallier ){
            if(!a.isUnlocked() && product.getQuantite()>=a.getSeuil()){
                majPallier(a,product);
        }
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
        //debloquer le manager de ce produit
        product.setManagerUnlocked(true);

// soustraire de l'argent du joueur le cout du manager
        double prixm = manager.getSeuil();
        double argent = world.getMoney();
        double argentRestant = argent - prixm;
        world.setMoney(argentRestant);
// sauvegarder les changements au monde
        saveWorldToXml(world, username);
        return true;
    }
    
    
    
    
     public Boolean updateUpgrades(String username, PallierType upgrade) throws JAXBException, FileNotFoundException {
         World world = getWorld(username);
         
         if(world.getMoney()>=upgrade.getSeuil() && !upgrade.isUnlocked()){
             if(upgrade.getIdcible()==0){
                 List<ProductType> listeProduits = world.getProducts().getProduct();
                 for(ProductType p : listeProduits){
                     majPallier(upgrade,p);
                 }
                 return true;
             }
             else {
                 ProductType p = findProductById(world, upgrade.getIdcible());
                 majPallier(upgrade,p);
                 return true;
             }
         }
         return false;
     }
    private ProductType findProductById(World world, int id) {
        ProductType produit = null;
        List<ProductType> products = world.getProducts().getProduct();
        for (ProductType p : products) {
            if (p.getId() == id) {
                produit = p;
            }
        }
        return produit;
    }

    private PallierType findManagerByName(World world, String name) {
        PallierType manager = null;
        List<PallierType> palliers = world.getManagers().getPallier();
        for (PallierType m : palliers) {
            if (m.getName().equals(name)) {
                manager = m;
            }
        }
        return manager;
    }
    
    public void majPallier(PallierType a, ProductType product){
        
        if(a.isUnlocked()==false && product.getQuantite()>=a.getSeuil()){
                a.setUnlocked(true);
                if(a.getTyperatio()==TyperatioType.VITESSE){
                   int vit =  product.getVitesse();
                    vit=(int)(vit*a.getRatio());
                    product.setVitesse(vit);
                }
                else {
                    if(a.getTyperatio()== TyperatioType.GAIN){
                    double rev=product.getRevenu();
                    rev = rev*a.getRatio();
                    product.setRevenu(rev);
                    }
                }
            }
    }
    
    public void angelUpgrade(String username, PallierType ange)throws JAXBException, FileNotFoundException {
        int a=ange.getSeuil();
        World world = getWorld(username);
        double angeActifs=world.getActiveangels();
        
        double newAngeActifs = angeActifs-a;
        
        if(ange.getTyperatio()==TyperatioType.ANGE){
            int angelBonus = world.getAngelbonus();
            angelBonus+=angelBonus+ange.getRatio();
            world.setAngelbonus(angelBonus);
        }
        else {
            updateUpgrades(username,ange);
        }
        
        world.setActiveangels(newAngeActifs);
        
        
        
    }

}
