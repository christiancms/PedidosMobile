package br.com.ezzysoft.cardapiomobile.fragments;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import br.com.ezzysoft.cardapiomobile.R;
import br.com.ezzysoft.cardapiomobile.adapters.ProdutoExpandableListAdapter;
import br.com.ezzysoft.cardapiomobile.adapters.ProdutosListAdapter;
import br.com.ezzysoft.cardapiomobile.bean.ItemPedido;
import br.com.ezzysoft.cardapiomobile.bean.Pedido;
import br.com.ezzysoft.cardapiomobile.bean.Produto;
import br.com.ezzysoft.cardapiomobile.ws.ProdutoHttp;

/**
 * Created by christian on 30/06/17.
 */

public class ProdutosListFragment extends Fragment {


    TextView mTextMesagem;
    ProgressBar mProgressBar;
    ListView mListView;
    ExpandableListView eListView;
    List<String> listRs = new ArrayList<String>();
    Map<String, List<String>> dados = new HashMap<String, List<String>>();
    List<Produto> mProdutos;
    EditText mMesa;
    ProdutosTask mTask;
    private Button mSubTotal;
    List<ItemPedido> itensPedidoList;
    ProdutosListAdapter pla;
    Pedido ped = new Pedido();
    Boolean enviar = false;
    String docXml = "";
    private String endereco;
    private String porta;

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public String getPorta() {
        return porta;
    }

    public void setPorta(String porta) {
        this.porta = porta;
    }

    public List<ItemPedido> getItensPedidoList() {
        return itensPedidoList;
    }

    public Button getmSubTotal() {
        return mSubTotal;
    }

    public void setmSubTotal(Button mSubTotal) {
        this.mSubTotal = mSubTotal;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_produtos_list, container, false);

        mTextMesagem = (TextView) layout.findViewById(android.R.id.empty);
        mProgressBar = (ProgressBar) layout.findViewById(R.id.progressBar);
        eListView = (ExpandableListView) layout.findViewById(R.id.expandableListView);
        eListView.setEmptyView(mTextMesagem);
        mListView = (ListView) layout.findViewById(R.id.list);
        mListView.setEmptyView(mTextMesagem);
        mMesa = (EditText) layout.findViewById(R.id.inputSearch);

        mSubTotal = (Button) layout.findViewById(R.id.btnSubTotal);

        mSubTotal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mMesa.getText().toString().equals("") || mSubTotal.getText().equals("0")) {
                    String intx = "";
                    Long codItem = 0L;
                    FragmentManager fragmentManager = getFragmentManager();
                    ped.setColaboradorId(1L);
                    ped.setMesa(Integer.parseInt(mMesa.getText().toString()));
                    Toast.makeText(getActivity(), "Subtotal: " + mSubTotal.getText(), Toast.LENGTH_SHORT).show();
                    pla = (ProdutosListAdapter) mListView.getAdapter();
                    ItemPedido item;
                    itensPedidoList = new ArrayList<>();
                    Iterator<Map.Entry<Integer, String[]>> iterator = pla.getItensMap().entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<Integer, String[]> reg = iterator.next();
                        item = new ItemPedido();
                        codItem++;
                        Double y = Double.parseDouble(reg.getValue()[1]);
                        if (y > 0d) {
                            try {
                                intx = "" + reg.getKey();
                            } catch (ClassCastException e) {
                                e.printStackTrace();
                            }
                            item.setIdItem(codItem);
                            item.setProdutoId(Integer.valueOf(intx));
                            Double vlr = Double.parseDouble((reg.getValue()[0]).replace(",", "."));
                            item.setVlrUnitProduto(vlr); // [0] - Double
                            item.setQuantidade(reg.getValue()[1] != null ? Double.parseDouble(reg.getValue()[1]) : 0d);// [1] - Double
                            itensPedidoList.add(item);
                        }
                    }

                    String subTotal = mSubTotal.getText().toString();
                    String mesa = mMesa.getText().toString();

                    Bundle bundle = new Bundle();
                    bundle.putString("mesa", mesa);
                    bundle.putString("mSubTotal", subTotal);
                    PedidoListFragment pelf = new PedidoListFragment();
                    pelf.setArguments(bundle);
                    List<ItemPedido> mLista;
                    pelf.setmItensPedido(itensPedidoList);

                    fragmentManager.beginTransaction()
                            .replace(R.id.content_frame, pelf)
                            .addToBackStack(null)
                            .commit();
                } else {
                    Toast.makeText(getActivity(), "Informe a mesa", Toast.LENGTH_LONG).show();
                }
            }
        });
        return layout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mProdutos == null) {
            mProdutos = new ArrayList<>();
        }

        if (mTask == null) {
            if (ProdutoHttp.temConexao(getActivity())) {
                iniciarDownload();
            } else {
                mTextMesagem.setText("Sem conexão");
            }
        } else if (mTask.getStatus() == AsyncTask.Status.RUNNING) {
            exibirProgress(true);
        }
    }

    private void exibirProgress(boolean exibir) {
        if (exibir) {
            mTextMesagem.setText("Baixando informações dos produtos...");
        }
        mTextMesagem.setVisibility(exibir ? View.VISIBLE : View.GONE);
        mProgressBar.setVisibility(exibir ? View.VISIBLE : View.GONE);
    }

    public void iniciarDownload() {
        if (mTask == null || mTask.getStatus() != AsyncTask.Status.RUNNING) {
            mTask = new ProdutosTask();
            mTask.execute();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        List<String> listagemGrupos = new ArrayList<>();
        listRs.add("Pelotas");
        listRs.add("Porto Alegre");
        listRs.add("Rio Grande");
        if (!mProdutos.isEmpty()){
            for (Produto p: mProdutos) {
                if (!listagemGrupos.contains(p.getNomeGrupo())) {
                    listagemGrupos.add(p.getNomeGrupo());
                }
            }// dados.put(nome, );
            // Ordem o nome dos grupos em ordem alfabética, crescente
            Collections.sort(listagemGrupos);
            for (String nome :listagemGrupos) {
                dados.put(nome, listagemGrupos);
            }
        }
        final ProdutosListAdapter mAdapter = new ProdutosListAdapter(this.getActivity(), mProdutos, this);
        mListView.setAdapter(mAdapter);
        eListView.setAdapter(new ProdutoExpandableListAdapter(dados));
    }

    class ProdutosTask extends AsyncTask<Void, Void, List<Produto>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            exibirProgress(true);
        }

        @Override
        protected List<Produto> doInBackground(Void... strings) {
            return ProdutoHttp.carregarProdutosJson(getEndereco() + ":" + getPorta());
//            return ProdutoHttp.carregarProdutosXml(getEndereco() + ":" + getPorta());

        }

        @Override
        protected void onPostExecute(List<Produto> produtos) {
            super.onPostExecute(produtos);
            exibirProgress(false);
            if (produtos != null) {
                mProdutos.clear();
                mProdutos.addAll(produtos);
            } else {
                mTextMesagem.setText("Falha ao obter produtos");
            }
            onStart();

        }
    }
}
//                    XmlSerializer serializer = Xml.newSerializer();
//                    StringWriter writer = new StringWriter();
//                    try {
//                        serializer.setOutput(writer);
//                        // start document
//                        serializer.startDocument("UTF-8", true);
//                        serializer.startTag("", "pedido");
//                        serializer.startTag("", "garcom");
//                        serializer.text("" + ped.getColaboradorId());
//                        serializer.endTag("", "garcom");
//                        serializer.startTag("", "mesa");
//                        serializer.text("" + ped.getMesa());
//                        serializer.endTag("", "mesa");
//                        serializer.startTag("", "itenspedido");
//                        for (ItemPedido elem : itensPedidoList) {
//                            serializer.startTag("", "item");
//                            serializer.startTag("", "codigoProduto");
//                            serializer.text("" + elem.getIdProduto());
//                            serializer.endTag("", "codigoProduto");
//                            serializer.startTag("", "quantidade");
//                            serializer.text("" + elem.getQtdeProduto());
//                            serializer.endTag("", "quantidade");
//                            serializer.endTag("", "item");
//                        }
//                        serializer.endTag("", "itenspedido");
//                        serializer.endTag("", "pedido");
//                        serializer.endDocument();
//                        // end document.
//
//                    } catch (IllegalArgumentException e) {
//                        e.printStackTrace();
//                    } catch (IllegalStateException e) {
//                        e.printStackTrace();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    docXml = writer.toString();
//                    if (mTasks == null){
//                        mTasks = new PedidoTask();
//                        mTasks.execute();
//                    }else {
//                        mTasks.execute();
//                    }
