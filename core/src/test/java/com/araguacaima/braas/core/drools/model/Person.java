package com.araguacaima.braas.core.drools.model;

import com.araguacaima.braas.core.drools.Commons;
import io.codearte.jfairy.producer.company.Company;
import io.codearte.jfairy.producer.person.Address;
import org.joda.time.DateTime;

import java.lang.reflect.InvocationTargetException;

public class Person extends io.codearte.jfairy.producer.person.Person {

    public Person(String firstName, String middleName, String lastName, Address address, String email, String username, String password, Sex sex, String telephoneNumber, DateTime dateOfBirth, Integer age, String nationalIdentityCardNumber, String nationalIdentificationNumber, String passportNumber, Company company, String companyEmail) {
        super(firstName, middleName, lastName, address, email, username, password, sex, telephoneNumber, dateOfBirth, age, nationalIdentityCardNumber, nationalIdentificationNumber, passportNumber, company, companyEmail);
    }

    public void setAddress(Address address) throws InvocationTargetException, IllegalAccessException {
        Commons.setProperty(this, "address", address);
    }

    public void setFirstName(String firstName) throws InvocationTargetException, IllegalAccessException {
        Commons.setProperty(this, "firstName", firstName);
    }

    public void setMiddleName(String middleName) throws InvocationTargetException, IllegalAccessException {
        Commons.setProperty(this, "middleName", middleName);
    }

    public void setLastName(String lastName) throws InvocationTargetException, IllegalAccessException {
        Commons.setProperty(this, "lastName", lastName);
    }

    public void setEmail(String email) throws InvocationTargetException, IllegalAccessException {
        Commons.setProperty(this, "email", email);
    }

    public void setUsername(String username) throws InvocationTargetException, IllegalAccessException {
        Commons.setProperty(this, "username", username);
    }

    public void setPassword(String password) throws InvocationTargetException, IllegalAccessException {
        Commons.setProperty(this, "password", password);
    }

    public void setSex(Sex sex) throws InvocationTargetException, IllegalAccessException {
        Commons.setProperty(this, "sex", sex);
    }

    public void setTelephoneNumber(String telephoneNumber) throws InvocationTargetException, IllegalAccessException {
        Commons.setProperty(this, "telephoneNumber", telephoneNumber);
    }

    public void setDateOfBirth(DateTime dateOfBirth) throws InvocationTargetException, IllegalAccessException {
        Commons.setProperty(this, "dateOfBirth", dateOfBirth);
    }

    public void setAge(Integer age) throws InvocationTargetException, IllegalAccessException {
        Commons.setProperty(this, "age", age);
    }

    public void setCompany(Company company) throws InvocationTargetException, IllegalAccessException {
        Commons.setProperty(this, "company", company);
    }

    public void setCompanyEmail(String companyEmail) throws InvocationTargetException, IllegalAccessException {
        Commons.setProperty(this, "companyEmail", companyEmail);
    }

    public void setNationalIdentityCardNumber(String nationalIdentityCardNumber) throws InvocationTargetException, IllegalAccessException {
        Commons.setProperty(this, "nationalIdentityCardNumber", nationalIdentityCardNumber);
    }

    public void setNationalIdentificationNumber(String nationalIdentificationNumber) throws InvocationTargetException, IllegalAccessException {
        Commons.setProperty(this, "nationalIdentificationNumber", nationalIdentificationNumber);
    }

    public void setPassportNumber(String passportNumber) throws InvocationTargetException, IllegalAccessException {
        Commons.setProperty(this, "passportNumber", passportNumber);
    }

    public static class PersonWrapper {
        public static Person fromParent(io.codearte.jfairy.producer.person.Person person) {
            return new Person(person.getFirstName(),
                    person.getMiddleName(),
                    person.getLastName(),
                    person.getAddress(),
                    person.getEmail(),
                    person.getUsername(),
                    person.getPassword(),
                    person.getSex(),
                    person.getTelephoneNumber(),
                    person.getDateOfBirth(),
                    person.getAge(),
                    person.getNationalIdentityCardNumber(),
                    person.getNationalIdentificationNumber(),
                    person.getPassportNumber(),
                    person.getCompany(),
                    person.getCompanyEmail());
        }
    }

}
