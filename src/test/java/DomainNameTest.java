
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author jitta
 */
public class DomainNameTest {

    public static void main(String[] args) throws URISyntaxException, MalformedURLException {

        String host = getDomainName("https://api.pamarin.com/account");

        System.out.println("host => " + host);

    }

    public static String getDomainName(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String domain = uri.getHost();
        String[] arr = domain.split("\\.");
        System.out.println("arr => " + arr);
        if(arr.length == 2){
            return "www." + domain;
        }
        return domain;
    }
}
