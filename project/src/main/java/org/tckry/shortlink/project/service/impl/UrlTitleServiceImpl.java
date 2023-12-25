package org.tckry.shortlink.project.service.impl;

import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.tckry.shortlink.project.service.UrlTitleService;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-24 19:56
 **/
@Service
public class UrlTitleServiceImpl implements UrlTitleService {
    @SneakyThrows
    @Override
    public String getTitleByUrl(String url) {
        URL targetURL = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) targetURL.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        int responseCode = connection.getResponseCode();
        if (responseCode==HttpURLConnection.HTTP_OK){
            Document document = Jsoup.connect(url).get();
            return document.title();
        }
        return "Error while fetching title";
    }
}
