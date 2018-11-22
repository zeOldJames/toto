/*
 * Copyright 2014
 * 
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.framework3eclipse.creator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import com.github.framework3eclipse.dto.MethodInsertionPoint;

/**
 * Classe permettant de générer une méthode toString pour le framework 3.
 * 
 * @author x175567
 *
 */
public class ToStringMethodCreator extends AbstractMethodCreator {

	private boolean useArrays = false;

	public ToStringMethodCreator(MethodInsertionPoint insertionPoint, List<String> fields) throws JavaModelException {
		super(insertionPoint, fields);
	}

	/** {@inheritDoc} */
	@Override
	protected String getMethodContent() throws JavaModelException {

		StringBuilder content = new StringBuilder();
		List<String> basicField = new ArrayList<>();
		List<String> complexeField = new ArrayList<>();
		List<String> collectionField = new ArrayList<>();

		// ajout de la partie "header" du toString
		printToStringHeader(content);

		// récupération des champs de la classe
		IField[] iFields = insertionPoint.getInsertionType().getFields();
		
		// itération sur les noms des champs sélectionnés pour déterminer les champs, simple, les champs complexe et les champs "collection"
		for (String field : fields) {
			for (int i = 0; i < iFields.length; i++) {
				// match en nom de champs sélectionné et champs de la classe
				if (field.equals(iFields[i].getElementName())) {
					// récupération de la signature du champs 
					// (la signature  récupéré n'est pas le vrai type "java" de l'objet mais seulement ce qui est indiqué à gauche du nom du champs dans les sources)
					// (on va donc devoir déterminer ensuite son vrai type en fonction des imports, si existant dans java.lang)
					
					String type = Signature.getSignatureSimpleName(iFields[i].getTypeSignature());
					String simpleType = "";
					
					// on vérifie si la signature est un template, suppression de la partie template si c'est le cas
					if (isGeneric(type)) {
						simpleType = nameWithoutGeneric(type);
					} else {
						simpleType = type;
					}

					// si le type appartien à java.lang ou si il appartient au framework sogecap, on le met en tant que champ "simple"
					// si le type appartient à java.util.List, on le met en tant que collection
					// sinon on le met en tant que champ complexe
					if (isJavaLang(simpleType) || getQualifiedName(simpleType).startsWith("com.sogecap.framework.")) {
						basicField.add(field);
					} else if (getQualifiedName(simpleType).equals("java.util.List")) {
						collectionField.add(field);
					} else {
						complexeField.add(field);
					}

					break;
				}
			}
		}

		// on ajoute les différents de champs au toPrint 
		printToStringSimpleField(content, basicField);
		printToStringCollectionField(content, collectionField, iFields);
		printToStringComplexeField(content, complexeField);
		
		// on ajoute le "footer" de la méthode toString
		printToStringFooter(content);
		return content.toString();
	}

	/** {@inheritDoc} */
	@Override
	protected String getMethodToDelete() {
		return "toString";
	}

	/** {@inheritDoc} */
	@Override
	protected String getPackageToImport() {
		String packageToImport = super.getPackageToImport();
		if (useArrays) {
			return packageToImport + "," + IMPORT_DECL_ARRAYS;
		} else {
			return packageToImport;
		}
	}

	/**
	 * Méthode ajoutant au content le début de la méthode toString.
	 * @param content StringBuilder auquel on ajoute les champs simlple à afficher
	 */
	private void printToStringHeader(StringBuilder content) {
		content.append("@Override\n");
		content.append("public String toString()\n");
		content.append("{\n");
		content.append("\treturn new ToStringLogger(this.getClass())\n");
	}

	/**
	 * Méthode ajoutant au content les champs simple à afficher.
	 * @param content StringBuilder auquel on ajoute l'entête de la méthode toString
	 * @param fields champs à ajouter
	 */
	private void printToStringSimpleField(StringBuilder content, List<String> fields) {
		for (final String field : fields) {
			content.append("\t\t\t.append(\"").append(field).append("\", this.").append(field).append(")\n");
		}
	}

	/**
	 * Méthode ajoutant au content les champs de type collection à afficher.
	 * @param content StringBuilder auquel on ajoute les champs "collection" à afficher
	 * @param fields nom des champs à afficher
	 * @param iFields champs associés à la classe pou laquelle on génère l'affichage
	 * @throws JavaModelException
	 */
	private void printToStringCollectionField(StringBuilder content, List<String> fields, IField[] iFields)
			throws JavaModelException {		
		// pour chaque selectionné, on conditionne la façon d'afficher le champ si il est du type liste de string.
		for (String field : fields) {
			for (int i = 0; i < iFields.length; i++) {
				if (field.equals(iFields[i].getElementName())) {
					String type = Signature.getSignatureSimpleName(iFields[i].getTypeSignature());
					if ("String".equals(getTypeArgument(type))) {
						content.append("\t\t\t.append(\"").append(field).append("\", this.").append(field)
								.append(")\n");
					} else {
						content.append("\t\t\t.append(this.").append(field).append(")\n");
					}
					break;
				}
			}
		}
	}

	/**
	 * Méthode ajoutant au content les champs de type compelxe à afficher.
	 * @param content StringBuilder auquel on ajoute les champs complexe à afficher
	 * @param fields champs associés à la classe pou laquelle on génère l'affichage
	 * @throws JavaModelException
	 */
	private void printToStringComplexeField(StringBuilder content, List<String> fields) throws JavaModelException {
		for (String field : fields) {
			content.append("\t\t\t.append(this.").append(field).append(")\n");
		}
	}

	/**
	 * Méthode affichant la fin de la méthode toString
	 * @param content StringBuilder auquel on ajoute la fin de la méthode toString
	 */
	private void printToStringFooter(StringBuilder content) {
		content.append("\t\t\t.toString();\n");
		content.append("}\n");
	}

	/**
	 * Méthode qui cherche si le type est dans les imports ou non pour avoir le type "qualifié" du champ (fr.**.Type au lieu Type) 
	 * @param name nom du type trouvé pour unchamp
	 * @return le type qualifié si trouvé dans les imports sinnon le type en entrée de méthode
	 * @throws JavaModelException
	 */
	private String getQualifiedName(String name) throws JavaModelException {
		for (String importe : getImports()) {
			if (importe.endsWith("." + name)) {
				return importe;
			}
		}
		return name;
	}

	/**
	 * Métode vérifiant si le nom du type en entrée existe dans le classLoader
	 * @param name nom du type
	 * @return true si il appartient à java.lang
	 */
	private boolean isJavaLang(String name) {
		try {
			this.getClass().getClassLoader().loadClass("java.lang." + name);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	/**
	 * Vérifie si un type est générique (name<T1>) en fonction de son nom 
	 * @param name nom du type
	 * @return true si il est de type générique
	 */
	private boolean isGeneric(String name) {
		return name.contains("<");
	}

	/**
	 * 
	 * @param name nom du type générique (name<T1>)
	 * @return le nom du type sans la partie générique (name)
	 */
	private String nameWithoutGeneric(String name) {
		return name.substring(0, name.indexOf("<"));
	}

	/**
	 * Méthode récupérant les imports de la classe.
	 * @return les imports sous forme de liste
	 * @throws JavaModelException
	 */
	private List<String> getImports() throws JavaModelException {
		List<String> liste = new ArrayList<>();
		ICompilationUnit compilationUnit = getCompilationUnit();
		if (compilationUnit != null) {
			for (IImportDeclaration importDecl : compilationUnit.getImports()) {

				liste.add(importDecl.getElementName());
			}
		}
		return liste;
	}

	/**
	 * Méthode récupérant le type de nom présent dans le type générique d'un champ. (fonctionne uniquement avec les listes pour le moment).
	 * @param type nom du type générique (name<T1>)
	 * @return nom du type présent dans le type générique (T1)
	 */
	private String getTypeArgument(String type) {
		String regex = ".*<(.*)>";

		Matcher matcher = Pattern.compile(regex).matcher(type);
		if (matcher.matches() && matcher.groupCount() > 0) {
			return matcher.group(1);
		}
		return "";
	}

}
