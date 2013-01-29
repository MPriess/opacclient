package de.geeksfactory.opacclient.apis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.storage.MetaDataSource;

public class Biber implements OpacApi {

	private DefaultHttpClient ahc;
	private String opac_url = "http://opac.karlsruhe.de/opac";
	private MetaDataSource metaData;
	private String results;
	private JSONObject data;
	private Context context;
	private boolean initialised = false;
	private String last_error;
	private Library library;
	private String searchObj;
	private static final int pageSize = 20;

	@Override
	public String[] getSearchFields() {
		return new String[] { "verfasser", // AW
				"titel", // TW
				"zweigstelle", // PP
				"isbn", // IS
				"verlag" }; // PU
	}

	@Override
	public String getLast_error() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getResults() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAccountSupported(Library library) {
		return false;
	}

	@Override
	public boolean isAccountExtendable() {
		return false;
	}

	@Override
	public String getAccountExtendableInfo(Account acc)
			throws ClientProtocolException, SocketException, IOException,
			NotReachableException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void start() throws ClientProtocolException, IOException,
			NotReachableException, SocketException {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void init(MetaDataSource metadata, Library library) {
		ahc = new DefaultHttpClient();
		
		this.library = library;

		try {
			this.opac_url = library.getData().getString("baseurl");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public List<SearchResult> search(Bundle query) throws IOException,
			NotReachableException {

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("LANG", "de"));
		params.add(new BasicNameValuePair("FUNC", "qsel"));

		params.add(new BasicNameValuePair("REG1", "AW"));
		params.add(new BasicNameValuePair("FLD1", query.getString("verfasser")));
		params.add(new BasicNameValuePair("CNN1", "AND"));

		params.add(new BasicNameValuePair("REG2", "TW"));
		params.add(new BasicNameValuePair("FLD2", query.getString("titel")));
		params.add(new BasicNameValuePair("CNN2", "AND"));

		params.add(new BasicNameValuePair("REG3", "PP"));
		params.add(new BasicNameValuePair("FLD3", query
				.getString("zweigstelle")));
		params.add(new BasicNameValuePair("CNN3", "AND"));

		params.add(new BasicNameValuePair("REG4", "IS"));
		params.add(new BasicNameValuePair("FLD4", query.getString("isbn")));
		params.add(new BasicNameValuePair("CNN4", "AND"));

		params.add(new BasicNameValuePair("REG5", "PU"));
		params.add(new BasicNameValuePair("FLD5", query.getString("verlag")));
		params.add(new BasicNameValuePair("SHOW", String.valueOf(pageSize)));
		
		// String requestUrl = opac_url + URLEncodedUtils.format(params,
		// "UTF-8");

		String requestUrl = opac_url + "/g_query.S?"
				+ URLEncodedUtils.format(params, "UTF-8");

		Log.d("Search Request", requestUrl);
		
		searchObj = requestUrl;

		HttpGet request = new HttpGet(requestUrl);

		HttpResponse response = ahc.execute(request);

		String html = convertStreamToString(response.getEntity().getContent());

		return parseSearch(html);
	}

	private List<SearchResult> parseSearch(String html) {
		List<SearchResult> results = new ArrayList<SearchResult>();
		Document doc = Jsoup.parse(html);

		Elements elements = doc.select("html body table tbody tr td#main dl");

		for (Element element : elements) {
			Elements titles = element.select("dt a");
			Elements locations = element.select("dd span.klein");
			
			Iterator<Element> locationIterator = locations.iterator();
			
			for (Element title : titles) {
				Element location = locationIterator.next();
				
				SearchResult searchResult = new SearchResult();
				
				String innerHtml = title.toString() + "<br/>" + location;
				
				innerHtml = innerHtml.replace("class=\"p8\"", "color=\"red\"");
				innerHtml = innerHtml.replace("class=\"p2\"", "color=\"green\"");
				
				System.out.println("Example: " + innerHtml);
				
				searchResult.setInnerhtml(innerHtml);
				
				searchResult.setId(title.attr("href"));
				results.add(searchResult);
			}
		}

		return results;
	}

	private String convertStreamToString(InputStream is) throws IOException {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(is, "ISO-8859-1"));
		} catch (UnsupportedEncodingException e1) {
			reader = new BufferedReader(new InputStreamReader(is));
		}
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append((line + "\n"));
			}
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	@Override
	public List<SearchResult> searchGetPage(int page) throws IOException,
			NotReachableException {
		
		String url = searchObj + "&FROMPOS=" + (((page - 1) * 20) + 1);
		
		System.out.println("PAGING:" + url);
		
		HttpGet request = new HttpGet(url);

		HttpResponse response = ahc.execute(request);

		String html = convertStreamToString(response.getEntity().getContent());

		return parseSearch(html);
	}

	@Override
	public DetailledItem getResultById(String id) throws IOException,
			NotReachableException {
		DetailledItem item = new DetailledItem();

		URL url = new URL(opac_url + "/" + id);

		Document document = Jsoup.parse(url, 5000);

		Elements rows = document
				.select("html body table tbody tr td#main table.liste tbody tr");
		for (Element row : rows) {
			Elements columns = row.select("td");

			String firstColumn = "";

			for (Element element : columns) {

				if (isEmpty(element)) {
					firstColumn = "";
				}

				if (firstColumn.equals("Titel")) {
					item.setTitle(element.text());
				}

				else if (firstColumn.equals("Verfasser")) {
					item.setTitle(element.text());
				}

				else if (firstColumn.equals("ISBN")) {
					Detail detail = new Detail("ISBN", element.text());
					item.getDetails().add(detail);
				}

				else if (firstColumn.equals("Thema")) {
					Detail detail = new Detail("Thema", element.text());
					item.getDetails().add(detail);
				}

				else if (firstColumn.equals("Standort")) {
					Detail detail = new Detail("Standort", element.text());
					item.getDetails().add(detail);
				}

				else if (firstColumn.equals("Serie")) {
					Detail detail = new Detail("Serie", element.text());
					item.getDetails().add(detail);
				}

				else if (firstColumn.equals("Notation")) {
					Detail detail = new Detail("Notation", element.text());
					item.getDetails().add(detail);
				}

				else if (firstColumn.equals("Bestände")) {
					Detail detail = new Detail("Bestände", element.text());

					item.getDetails().add(detail);
				}

				firstColumn = element.text();
			}
		}

		return item;
	}

	private boolean isEmpty(Element element) {
		return (element == null || element.text().trim().length() == 0);
	}

	@Override
	public DetailledItem getResult(int position) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReservationResult reservation(String branch, Account account)
			throws IOException {
		return null;
	}

	@Override
	public boolean prolong(Account account, String media) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean cancel(Account account, String media) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public AccountData account(Account account) throws IOException,
			JSONException {
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		
		params.add(new BasicNameValuePair("LANG", "DE"));
		params.add(new BasicNameValuePair("FUNC", "medk"));

		params.add(new BasicNameValuePair("BENUTZER", account.getName()));

		params.add(new BasicNameValuePair("PASSWORD", account.getPassword()));

		
		String requestUrl = opac_url + "/g2_user.S"
				+ URLEncodedUtils.format(params, "UTF-8");
		
		HttpPost request = new HttpPost(requestUrl);
		
		HttpResponse response = ahc.execute(request);
		
		String html = convertStreamToString(response.getEntity().getContent());
		return parseAccount(account, html);
	}
	
	private AccountData parseAccount(Account account, String html) {
		AccountData data = new AccountData(account.getId());
		
		Document doc = Jsoup.parse(html);
		
		Elements row = doc.select("/html/body/table/tbody/tr/td[3]/form/table/tbody/tr");
		
		System.out.println(row);
		
		List<ContentValues> medien = new ArrayList<ContentValues>();
		
		ContentValues medium = new ContentValues();		
		medium.put(AccountData.KEY_LENT_DEADLINE, "04.02.2013");
		medium.put(AccountData.KEY_LENT_TITLE, "Vcl 3 / Koche/ Kochen mit Weight Watchers");
		
		medien.add(medium);
		medien.add(medium);
		medien.add(medium);
		medien.add(medium);
		data.setLent(medien);
		data.setReservations(medien);
		
		return data;
	}

}
