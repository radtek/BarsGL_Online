package ru.rbt.barsgl.common.xml;

/**
 * Created by Ivan Sevastyanov
 */

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import javax.xml.namespace.NamespaceContext;
import java.util.Iterator;
import java.util.Map;

/**

 Пример использования:
 <code>NamespaceContext nsc =
 new MapBasedNamespaceContext(
 ImmutableMap.<String, String>builder()
 .put("pos", "http://posting.kfd.gpb.ru")
 .put("bt", "http://basetypes.kfd.gpb.ru")
 .build()
 );</code>

 */
public class MapBasedNamespaceContext implements NamespaceContext {

    /**
     * [ prefix —> uri ]
     */
    private final Map<String, String> prefixToUri;

    /**
     * [ uri -> [prefix] ]
     */
    private final Multimap<String, String> uriToPrefixes;

    public MapBasedNamespaceContext(final Map<String, String> prefixToUri) {
        this.prefixToUri = ImmutableMap.copyOf(prefixToUri);

        final ImmutableMultimap.Builder<String, String> uriToPrefixes = ImmutableMultimap.builder();
        for (Map.Entry<String, String> e : prefixToUri.entrySet()) {
            uriToPrefixes.put(e.getValue(), e.getKey());
        }
        this.uriToPrefixes = uriToPrefixes.build();
    }

    public String getNamespaceURI(final String prefix) {
        return prefixToUri.get(prefix);
    }

    public String getPrefix(final String namespaceURI) {
        final Iterator<String> prefixes = uriToPrefixes.get(namespaceURI).iterator();
        return prefixes.hasNext() ? prefixes.next() : null;
    }

    public Iterator getPrefixes(final String namespaceURI) {
        return uriToPrefixes.get(namespaceURI).iterator();
    }
}
