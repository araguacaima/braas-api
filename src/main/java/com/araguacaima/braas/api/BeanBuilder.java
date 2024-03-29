package com.araguacaima.braas.api;

import com.araguacaima.braas.core.google.model.Account;
import org.pac4j.sparkjava.SparkWebContext;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.List;

public class BeanBuilder {

    //Basic
    private String title;

    //Examples
    private Object model;
    private String source;
    private String mainTitle;
    private String caption;
    private String fullDescription;
    private List steps;

    //Account
    private String login;
    private String email;
    private String avatar;
    private boolean authorized = false;

    //Editor
    private Boolean fullView = null;

    //Error
    private String message;
    private StackTraceElement[] stackTrace;

    //Diagrams
    private String key;
    private String color;
    private String group;
    private String text;
    private boolean isGroup = false;
    private String from;
    private String to;
    private List<Account> accounts = new ArrayList<>();
    private List<String> roles = new ArrayList<>();
    private List<String> header = new ArrayList<>();
    private boolean prototyper = false;
    private String prototypeId;
    private boolean loadDesktopPrototyper = false;
    private boolean loadDesktopApi = false;
    private String product;
    private String shortDescription;

    public String getTitle() {
        return title;
    }

    public Object getModel() {
        return model;
    }

    public String getSource() {
        return source;
    }

    public String getMainTitle() {
        return mainTitle;
    }

    public String getCaption() {
        return caption;
    }

    public String getFullDescription() {
        return fullDescription;
    }

    public List getSteps() {
        return steps;
    }

    public String getLogin() {
        return login;
    }

    public String getEmail() {
        return email;
    }

    public String getAvatar() {
        return avatar;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public Boolean getFullView() {
        return fullView;
    }

    public String getMessage() {
        return message;
    }

    public StackTraceElement[] getStackTrace() {
        return stackTrace;
    }

    public String getKey() {
        return key;
    }

    public String getColor() {
        return color;
    }

    public String getGroup() {
        return group;
    }

    public String getText() {
        return text;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getHeader() {
        return header;
    }

    public boolean isPrototyper() {
        return prototyper;
    }

    public String getPrototypeId() {
        return prototypeId;
    }

    public boolean isLoadDesktopPrototyper() {
        return loadDesktopPrototyper;
    }

    public boolean isLoadDesktopApi() {
        return loadDesktopApi;
    }

    public String getProduct() {
        return product;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void fixAccountInfo(Request request, Response response) {
        if (request != null && response != null) {
            final SparkWebContext context = new SparkWebContext(request, response);
            appendAccountInfo((Account) context.getRequest().getSession().getAttribute("account"));
        }
    }

    public BeanBuilder title(final String title) {
        this.title = title;
        return this;
    }

    public BeanBuilder model(final Object model) {
        this.model = model;
        return this;
    }

    public BeanBuilder source(final String source) {
        this.source = source;
        return this;
    }

    public BeanBuilder mainTitle(final String mainTitle) {
        this.mainTitle = mainTitle;
        return this;
    }

    public BeanBuilder caption(final String caption) {
        this.caption = caption;
        return this;
    }

    public BeanBuilder fullDescription(final String fullDescription) {
        this.fullDescription = fullDescription;
        return this;
    }

    public BeanBuilder steps(final List steps) {
        this.steps = steps;
        return this;
    }

    public BeanBuilder avatar(String avatar) {
        this.avatar = avatar;
        return this;
    }

    public BeanBuilder name(String name) {
        this.login = name;
        return this;
    }

    public BeanBuilder authorized(boolean authorized) {
        this.authorized = authorized;
        return this;
    }

    public BeanBuilder email(String email) {
        this.email = email;
        return this;
    }

    public BeanBuilder fullView(Boolean fullView) {
        this.fullView = fullView;
        return this;
    }

    public BeanBuilder message(String message) {
        this.message = message;
        return this;
    }

    public BeanBuilder stack(StackTraceElement[] stackTrace) {
        this.stackTrace = stackTrace;
        return this;
    }

    public BeanBuilder key(String key) {
        this.key = key;
        return this;
    }

    public BeanBuilder color(String color) {
        this.color = color;
        return this;
    }

    public BeanBuilder group(String group) {
        this.group = group;
        return this;
    }

    public BeanBuilder text(String text) {
        this.text = text;
        return this;
    }

    public BeanBuilder isGroup(boolean isGroup) {
        this.isGroup = isGroup;
        return this;
    }

    public BeanBuilder from(String from) {
        this.from = from;
        return this;
    }

    public BeanBuilder to(String to) {
        this.to = to;
        return this;
    }

    public BeanBuilder accounts(List<Account> accounts) {
        this.accounts = accounts;
        return this;
    }

    public BeanBuilder roles(List<String> roles) {
        this.roles = roles;
        return this;
    }

    private void appendAccountInfo(Account account) {
        if (account == null) {
            this.avatar(null).name(null).email(null).authorized(false);
        } else {
            String name = account.getLogin();
            String email = account.getEmail();
            this.name(name);
            this.email(email);
            this.authorized(account.isEnabled());
        }
    }

    public BeanBuilder header(List<String> header) {
        this.header = header;
        return this;
    }

    public BeanBuilder prototyper(boolean b) {
        this.prototyper = b;
        return this;
    }

    public BeanBuilder prototypeId(String prototypeId) {
        this.prototypeId = prototypeId;
        return this;
    }

    public BeanBuilder loadDesktopPrototyper(boolean loadDesktopPrototyper) {
        this.loadDesktopPrototyper = loadDesktopPrototyper;
        return this;
    }

    public BeanBuilder loadDesktopApi(boolean loadDesktopApi) {
        this.loadDesktopApi = loadDesktopApi;
        return this;
    }

    public BeanBuilder product(String product) {
        this.product = product;
        return this;
    }

    public BeanBuilder shortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
        return this;
    }
}
