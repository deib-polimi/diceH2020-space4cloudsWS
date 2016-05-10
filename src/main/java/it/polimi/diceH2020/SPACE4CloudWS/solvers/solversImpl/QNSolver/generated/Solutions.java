//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.05.10 at 10:34:35 AM CEST 
//


package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.QNSolver.generated;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="measure" maxOccurs="unbounded" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="station" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="class" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="measureType" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="meanValue" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="lowerLimit" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="upperLimit" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="successful" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="analyzedSamples" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="discardedSamples" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="precision" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="alfa" type="{http://www.w3.org/2001/XMLSchema}double" />
 *                 &lt;attribute name="maxSamples" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="nodeType" type="{http://www.w3.org/2001/XMLSchema}string" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute name="modelName" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="solutionMethod" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="modelDefinitionPath" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "measure"
})
@XmlRootElement(name = "solutions")
public class Solutions {

    protected List<Solutions.Measure> measure;
    @XmlAttribute(name = "modelName", required = true)
    protected String modelName;
    @XmlAttribute(name = "solutionMethod", required = true)
    protected String solutionMethod;
    @XmlAttribute(name = "modelDefinitionPath", required = true)
    protected String modelDefinitionPath;

    /**
     * Gets the value of the measure property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the measure property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMeasure().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Solutions.Measure }
     * 
     * 
     */
    public List<Solutions.Measure> getMeasure() {
        if (measure == null) {
            measure = new ArrayList<Solutions.Measure>();
        }
        return this.measure;
    }

    /**
     * Gets the value of the modelName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * Sets the value of the modelName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setModelName(String value) {
        this.modelName = value;
    }

    /**
     * Gets the value of the solutionMethod property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSolutionMethod() {
        return solutionMethod;
    }

    /**
     * Sets the value of the solutionMethod property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSolutionMethod(String value) {
        this.solutionMethod = value;
    }

    /**
     * Gets the value of the modelDefinitionPath property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getModelDefinitionPath() {
        return modelDefinitionPath;
    }

    /**
     * Sets the value of the modelDefinitionPath property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setModelDefinitionPath(String value) {
        this.modelDefinitionPath = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;attribute name="station" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="class" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="measureType" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="meanValue" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="lowerLimit" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="upperLimit" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="successful" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="analyzedSamples" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="discardedSamples" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="precision" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="alfa" type="{http://www.w3.org/2001/XMLSchema}double" />
     *       &lt;attribute name="maxSamples" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="nodeType" type="{http://www.w3.org/2001/XMLSchema}string" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class Measure {

        @XmlAttribute(name = "station", required = true)
        protected String station;
        @XmlAttribute(name = "class", required = true)
        protected String clazz;
        @XmlAttribute(name = "measureType", required = true)
        protected String measureType;
        @XmlAttribute(name = "meanValue")
        protected String meanValue;
        @XmlAttribute(name = "lowerLimit")
        protected String lowerLimit;
        @XmlAttribute(name = "upperLimit")
        protected String upperLimit;
        @XmlAttribute(name = "successful", required = true)
        protected String successful;
        @XmlAttribute(name = "analyzedSamples")
        protected String analyzedSamples;
        @XmlAttribute(name = "discardedSamples")
        protected String discardedSamples;
        @XmlAttribute(name = "precision")
        protected String precision;
        @XmlAttribute(name = "alfa")
        protected Double alfa;
        @XmlAttribute(name = "maxSamples")
        protected String maxSamples;
        @XmlAttribute(name = "nodeType")
        protected String nodeType;

        /**
         * Gets the value of the station property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getStation() {
            return station;
        }

        /**
         * Sets the value of the station property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setStation(String value) {
            this.station = value;
        }

        /**
         * Gets the value of the clazz property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getClazz() {
            return clazz;
        }

        /**
         * Sets the value of the clazz property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setClazz(String value) {
            this.clazz = value;
        }

        /**
         * Gets the value of the measureType property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getMeasureType() {
            return measureType;
        }

        /**
         * Sets the value of the measureType property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setMeasureType(String value) {
            this.measureType = value;
        }

        /**
         * Gets the value of the meanValue property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getMeanValue() {
            return meanValue;
        }

        /**
         * Sets the value of the meanValue property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setMeanValue(String value) {
            this.meanValue = value;
        }

        /**
         * Gets the value of the lowerLimit property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getLowerLimit() {
            return lowerLimit;
        }

        /**
         * Sets the value of the lowerLimit property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setLowerLimit(String value) {
            this.lowerLimit = value;
        }

        /**
         * Gets the value of the upperLimit property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getUpperLimit() {
            return upperLimit;
        }

        /**
         * Sets the value of the upperLimit property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setUpperLimit(String value) {
            this.upperLimit = value;
        }

        /**
         * Gets the value of the successful property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getSuccessful() {
            return successful;
        }

        /**
         * Sets the value of the successful property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setSuccessful(String value) {
            this.successful = value;
        }

        /**
         * Gets the value of the analyzedSamples property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getAnalyzedSamples() {
            return analyzedSamples;
        }

        /**
         * Sets the value of the analyzedSamples property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setAnalyzedSamples(String value) {
            this.analyzedSamples = value;
        }

        /**
         * Gets the value of the discardedSamples property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getDiscardedSamples() {
            return discardedSamples;
        }

        /**
         * Sets the value of the discardedSamples property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setDiscardedSamples(String value) {
            this.discardedSamples = value;
        }

        /**
         * Gets the value of the precision property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getPrecision() {
            return precision;
        }

        /**
         * Sets the value of the precision property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setPrecision(String value) {
            this.precision = value;
        }

        /**
         * Gets the value of the alfa property.
         * 
         * @return
         *     possible object is
         *     {@link Double }
         *     
         */
        public Double getAlfa() {
            return alfa;
        }

        /**
         * Sets the value of the alfa property.
         * 
         * @param value
         *     allowed object is
         *     {@link Double }
         *     
         */
        public void setAlfa(Double value) {
            this.alfa = value;
        }

        /**
         * Gets the value of the maxSamples property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getMaxSamples() {
            return maxSamples;
        }

        /**
         * Sets the value of the maxSamples property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setMaxSamples(String value) {
            this.maxSamples = value;
        }

        /**
         * Gets the value of the nodeType property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getNodeType() {
            return nodeType;
        }

        /**
         * Sets the value of the nodeType property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setNodeType(String value) {
            this.nodeType = value;
        }

    }

}
