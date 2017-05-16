package ru.rbt.barsgl.ejb.entity.gl;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static ru.rbt.barsgl.ejb.entity.gl.GLPosting.PostingType.*;

/**
 * Created by Ivan Sevastyanov
 */
@Entity
@Table(name = "GL_POSTING")
public class GLPosting extends BaseEntity<Long> {
    public enum PostingType {
        OneFilial("1"),
        ExchDiff("2"),
        MfoDebit("3"),
        MfoCredit("4"),
        FanMain("5");

        private final String value;

        private PostingType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
        public static PostingType parseType(String value) {
            for (PostingType postingType : values()) {
                if (postingType.getValue().equals(value)) return postingType;
            }
            return null;
        }
    }

    @Id
    @Column(name = "PCID")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "GLO_REF", nullable = false)
    private GLOperation operation;

    @Column(name = "POST_TYPE")
    private String postType;

    @Column(name = "STRN_PCID")
    private Long stornoPcid;

    @Transient
    List<Pd> pdList = new ArrayList<Pd>();

    public GLPosting() {
    }

    public GLPosting(Long id, GLOperation operation, PostingType postType) {
        this.id = id;
        this.operation = operation;
        this.postType = postType.getValue();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GLOperation getOperation() {
        return operation;
    }

    public void setOperation(GLOperation operation) {
        this.operation = operation;
    }

    public String getPostType() {
        return postType;
    }

    public void setPostType(String postType) {
        this.postType = postType;
    }

    public void setPostType(PostingType postType) {
        this.postType = postType.getValue();
    }

    public Long getStornoPcid() {
        return stornoPcid;
    }

    public void setStornoPcid(Long stornoPcid) {
        this.stornoPcid = stornoPcid;
    }

    public List<Pd> getPdList() {
        return pdList;
    }

    public void setPdList(List<Pd> pdList) {
        this.pdList = pdList;
    }

    public void addPd(Pd pd) {
        this.pdList.add(pd);
    }

    public void addPdList(List<Pd> pdAdd) {
        this.pdList.addAll(pdAdd);
    }

    public String getStornoType() {
        return getStornoTypeStatic(this.postType);
    }

    public static String getStornoTypeStatic(String postType) {
        if (postType.equals(MfoDebit.getValue())) {
            return MfoCredit.getValue();
        }
        else if (postType.equals(MfoCredit.getValue())) {
            return MfoDebit.getValue();
        }
        else {
            return postType;
        }
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
