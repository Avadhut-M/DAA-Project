import tkinter as tk
from tkinter import messagebox
from tkinter import ttk
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D
import networkx as nx
import random
import heapq
import time

INF = 999999

import json
import os

# ================= CACHE =================
CACHE_FILE = "learning_cache.json"

def load_cache():
    if os.path.exists(CACHE_FILE):
        try:
            with open(CACHE_FILE, "r") as f:
                return json.load(f)
        except:
            return {}
    return {}

def save_cache(cache):
    try:
        with open(CACHE_FILE, "w") as f:
            json.dump(cache, f, indent=4)
    except:
        pass

learning_cache = load_cache()

# ================= EDGE CLASS =================
class Edge:
    def __init__(self, u, v, w):
        self.u = u
        self.v = v
        self.w = w

# ================= DIJKSTRA =================
def dijkstra(V, graph, src, dest):
    start = time.perf_counter_ns()
    dist = [INF] * V
    parent = [-1] * V
    dist[src] = 0
    pq = [(0, src)]

    while pq:
        d, u = heapq.heappop(pq)
        
        # Improvement: Lazy deletion early exit
        if d > dist[u]:
            continue

        for edge in graph[u]:
            v = edge.v
            w = edge.w
            if dist[u] + w < dist[v]:
                dist[v] = dist[u] + w
                parent[v] = u
                heapq.heappush(pq, (dist[v], v))

    end = time.perf_counter_ns()
    return dist, parent, end - start

# ================= BELLMAN FORD =================
def bellman_ford(V, edges, src, dest):
    start = time.perf_counter_ns()
    dist = [INF] * V
    parent = [-1] * V
    dist[src] = 0

    # Improvement: Early stopping
    for _ in range(V - 1):
        updated = False
        for edge in edges:
            if dist[edge.u] != INF and dist[edge.u] + edge.w < dist[edge.v]:
                dist[edge.v] = dist[edge.u] + edge.w
                parent[edge.v] = edge.u
                updated = True
        if not updated:
            break
            
    # Improvement: Negative cycle detection
    has_negative_cycle = False
    for edge in edges:
        if dist[edge.u] != INF and dist[edge.u] + edge.w < dist[edge.v]:
            has_negative_cycle = True
            break

    end = time.perf_counter_ns()
    return dist, parent, end - start, has_negative_cycle

# ================= FLOYD WARSHALL =================
def floyd_warshall(V, edges, src, dest):
    start = time.perf_counter_ns()
    dist = [[INF] * V for _ in range(V)]
    nxt = [[-1] * V for _ in range(V)]
    
    for i in range(V):
        dist[i][i] = 0
        nxt[i][i] = i
        
    for edge in edges:
        if edge.w < dist[edge.u][edge.v]:
            dist[edge.u][edge.v] = edge.w
            nxt[edge.u][edge.v] = edge.v
        
    for k in range(V):
        for i in range(V):
            for j in range(V):
                if dist[i][k] != INF and dist[k][j] != INF and dist[i][k] + dist[k][j] < dist[i][j]:
                    dist[i][j] = dist[i][k] + dist[k][j]
                    nxt[i][j] = nxt[i][k]
                    
    has_negative_cycle = any(dist[i][i] < 0 for i in range(V))
    end = time.perf_counter_ns()
    
    return dist[src], nxt, end - start, has_negative_cycle

# ================= JOHNSON'S ALGORITHM =================
def johnsons(V, edges, src, dest):
    start = time.perf_counter_ns()
    
    # 1. Bellman-Ford from dummy node V
    dummy_edges = edges.copy()
    for i in range(V):
        dummy_edges.append(Edge(V, i, 0))
        
    h = [INF] * (V + 1)
    h[V] = 0
    for _ in range(V):
        for edge in dummy_edges:
            if h[edge.u] != INF and h[edge.u] + edge.w < h[edge.v]:
                h[edge.v] = h[edge.u] + edge.w
                
    has_negative_cycle = False
    for edge in dummy_edges:
        if h[edge.u] != INF and h[edge.u] + edge.w < h[edge.v]:
            has_negative_cycle = True
            break
            
    if has_negative_cycle:
        end = time.perf_counter_ns()
        return [INF]*V, [-1]*V, end - start, True
        
    # 2. Reweight edges
    reweighted_graph = [[] for _ in range(V)]
    for edge in edges:
        w_prime = edge.w + h[edge.u] - h[edge.v]
        reweighted_graph[edge.u].append(Edge(edge.u, edge.v, w_prime))
        
    # 3. Dijkstra for src node
    dist_prime = [INF] * V
    parent = [-1] * V
    dist_prime[src] = 0
    pq = [(0, src)]
    
    while pq:
        d, u = heapq.heappop(pq)
        if d > dist_prime[u]:
            continue
        for edge in reweighted_graph[u]:
            v = edge.v
            w = edge.w
            if dist_prime[u] + w < dist_prime[v]:
                dist_prime[v] = dist_prime[u] + w
                parent[v] = u
                heapq.heappush(pq, (dist_prime[v], v))
                
    # 4. Reverse reweighting
    dist = [INF] * V
    for i in range(V):
        if dist_prime[i] != INF:
            dist[i] = dist_prime[i] - h[src] + h[i]
            
    end = time.perf_counter_ns()
    return dist, parent, end - start, False

# ================= GET FW PATH =================
def get_fw_path(nxt, src, dest, dist_to_dest):
    if dist_to_dest == INF or nxt[src][dest] == -1:
        return []
    path = [src]
    u = src
    while u != dest:
        u = nxt[u][dest]
        if u == -1: return []
        path.append(u)
    return path

# ================= GET PATH =================
def get_path(parent, dest, dist_to_dest):
    # Improvement: check if path exists
    if dist_to_dest == INF:
        return []
        
    path = []
    curr = dest
    while curr != -1:
        path.append(curr)
        curr = parent[curr]
    return path[::-1]

# ================= OUTPUT =================
def write_output(text):
    output_text.config(state="normal")
    output_text.insert(tk.END, text)
    output_text.config(state="disabled")

# ================= GRAPH VISUALIZATION =================
def draw_graphs(V, edges, shortest_path=None, is_3d=False):
    G = nx.DiGraph()
    for i in range(V):
        G.add_node(i)

    for edge in edges:
        G.add_edge(edge.u, edge.v, weight=edge.w)

    pos = nx.spring_layout(G, dim=3 if is_3d else 2)

    # ================= 3D VIEW =================
    if is_3d:
        try:
            import plotly.graph_objects as go
        except ImportError:
            messagebox.showerror("Error", "Plotly is required for the new interactive 3D view. Please install it using 'pip install plotly'.")
            return

        edge_x, edge_y, edge_z = [], [], []
        path_x, path_y, path_z = [], [], []
        mid_x, mid_y, mid_z, mid_text = [], [], [], []

        path_edges = set()
        if shortest_path:
            for i in range(len(shortest_path) - 1):
                path_edges.add((shortest_path[i], shortest_path[i+1]))

        for edge in G.edges(data=True):
            u, v, data = edge
            w = data['weight']
            x0, y0, z0 = pos[u]
            x1, y1, z1 = pos[v]

            if (u, v) in path_edges:
                path_x.extend([x0, x1, None])
                path_y.extend([y0, y1, None])
                path_z.extend([z0, z1, None])
            else:
                edge_x.extend([x0, x1, None])
                edge_y.extend([y0, y1, None])
                edge_z.extend([z0, z1, None])

            # Edge weight labels at midpoint
            mx, my, mz = (x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2
            mid_x.append(mx)
            mid_y.append(my)
            mid_z.append(mz)
            mid_text.append(str(w))

        edge_trace = go.Scatter3d(
            x=edge_x, y=edge_y, z=edge_z,
            line=dict(width=3, color='gray'),
            hoverinfo='none', mode='lines', name='Edges'
        )

        path_trace = go.Scatter3d(
            x=path_x, y=path_y, z=path_z,
            line=dict(width=8, color='red'),
            hoverinfo='none', mode='lines', name='Shortest Path'
        )

        weight_trace = go.Scatter3d(
            x=mid_x, y=mid_y, z=mid_z,
            mode='text',
            text=mid_text,
            textposition='middle center',
            textfont=dict(color='darkred', size=14, family="Arial"),
            hoverinfo='none',
            name='Weights'
        )

        node_x, node_y, node_z, node_color, node_text = [], [], [], [], []
        for node in G.nodes():
            x, y, z = pos[node]
            node_x.append(x)
            node_y.append(y)
            node_z.append(z)
            node_text.append(str(node))
            is_in_path = shortest_path and node in shortest_path
            node_color.append('lightgreen' if is_in_path else 'skyblue')

        node_trace = go.Scatter3d(
            x=node_x, y=node_y, z=node_z,
            mode='markers+text',
            hoverinfo='text',
            text=node_text,
            textposition="middle center",
            textfont=dict(color='black', size=14, family="Arial"),
            marker=dict(size=14, color=node_color, line=dict(width=2, color='DarkSlateGrey')),
            name='Nodes'
        )

        fig = go.Figure(data=[edge_trace, path_trace, node_trace, weight_trace])
        
        annotations = []
        if shortest_path is not None and len(shortest_path) == 0:
            annotations.append(dict(
                x=0.5, y=0.95, xref='paper', yref='paper',
                text='NO PATH EXISTS BETWEEN SOURCE AND DESTINATION',
                showarrow=False,
                font=dict(size=18, color='white'),
                bgcolor='red',
                bordercolor='black',
                borderwidth=2,
                borderpad=4
            ))

        fig.update_layout(
            title="Interactive 3D Graph Visualization",
            showlegend=True,
            scene=dict(
                xaxis=dict(visible=False),
                yaxis=dict(visible=False),
                zaxis=dict(visible=False)
            ),
            margin=dict(l=0, r=0, b=0, t=40),
            annotations=annotations
        )
        fig.show(config={'displayModeBar': False, 'displaylogo': False})

    # ================= 2D VIEW =================
    else:
        fig, axes = plt.subplots(2, 1, figsize=(9, 11))

        # Original Graph
        nx.draw(G, pos, with_labels=True, node_color='skyblue', node_size=1500, font_weight='bold', arrows=True, connectionstyle="arc3,rad=0.1", ax=axes[0])
        edge_labels = nx.get_edge_attributes(G, 'weight')
        nx.draw_networkx_edge_labels(G, pos, edge_labels=edge_labels, label_pos=0.3, font_size=9, ax=axes[0])
        axes[0].set_title("Original Graph")

        # Shortest Path Graph
        node_colors = ['lightgreen' if shortest_path and node in shortest_path else 'skyblue' for node in G.nodes()]
        nx.draw(G, pos, with_labels=True, node_color=node_colors, node_size=1500, font_weight='bold', arrows=True, connectionstyle="arc3,rad=0.1", ax=axes[1])
        nx.draw_networkx_edge_labels(G, pos, edge_labels=edge_labels, label_pos=0.3, font_size=9, ax=axes[1])

        if shortest_path and len(shortest_path) > 1:
            path_edges = [(shortest_path[i], shortest_path[i + 1]) for i in range(len(shortest_path) - 1)]
            nx.draw_networkx_edges(G, pos, edgelist=path_edges, width=4, edge_color='red', ax=axes[1])
        elif shortest_path is not None and len(shortest_path) == 0:
            axes[1].text(0.5, 0.95, "NO PATH EXISTS BETWEEN SOURCE AND DESTINATION", 
                         horizontalalignment='center', verticalalignment='top', transform=axes[1].transAxes, 
                         color='white', fontsize=14, fontweight='bold', 
                         bbox=dict(facecolor='red', alpha=0.8, edgecolor='black', boxstyle='round,pad=0.5'))

        axes[1].set_title("Shortest Path Graph")
        plt.tight_layout()
        plt.show()

# ================= COMPARISON CHART =================
def comparison_chart(times):
    names = ["Dijkstra", "Bellman-Ford", "Floyd-Warshall", "Johnson's"]
    plt.figure(figsize=(9, 5))
    plt.bar(names, times, color=['blue', 'green', 'orange', 'purple'])
    plt.xlabel("Algorithms")
    plt.ylabel("Execution Time (ms)")
    plt.title("Execution Time Comparison")

    for i in range(len(times)):
        plt.text(i, times[i] + (max(times)*0.01), f"{times[i]:.5f}", ha='center', fontweight='bold')

    plt.tight_layout()
    plt.show()

# ================= RANDOM GRAPH =================
def generate_graph():
    try:
        if vertices_entry.get() and edges_entry.get():
            V = int(vertices_entry.get())
            E = int(edges_entry.get())
            max_edges = V * (V - 1)
            if E > max_edges:
                E = max_edges
                edges_entry.delete(0, tk.END)
                edges_entry.insert(0, str(E))
                messagebox.showinfo("Info", f"Maximum possible edges for {V} vertices is {max_edges}. Adjusted E to {max_edges}.")
        else:
            V = random.randint(8, 15)
            E = random.randint(V + 5, V * 2)
            vertices_entry.delete(0, tk.END)
            vertices_entry.insert(0, str(V))
            edges_entry.delete(0, tk.END)
            edges_entry.insert(0, str(E))

        src_val = source_entry.get()
        if not src_val:
            src = random.randint(0, V - 1)
            source_entry.delete(0, tk.END)
            source_entry.insert(0, str(src))
        else:
            src = int(src_val)

        dest_val = destination_entry.get()
        if not dest_val:
            dest = random.randint(0, V - 1)
            while src == dest and V > 1:
                dest = random.randint(0, V - 1)
            destination_entry.delete(0, tk.END)
            destination_entry.insert(0, str(dest))
        else:
            dest = int(dest_val)

        text_input.delete("1.0", tk.END)

        generated_edges = set()
        attempts = 0
        while len(generated_edges) < E and attempts < E * 10:
            attempts += 1
            u = random.randint(0, V - 1)
            v = random.randint(0, V - 1)
            if u != v and (u, v) not in generated_edges:
                w = random.randint(-3, 20)
                generated_edges.add((u, v))
                text_input.insert(tk.END, f"{u} {v} {w}\n")

    except Exception as e:
        messagebox.showerror("Error", f"Graph Generation Failed: {e}")

# ================= MAIN EXECUTION =================
def run_algorithms(best_only=False, cache_mode=False):
    try:
        if not vertices_entry.get() or not edges_entry.get() or not source_entry.get() or not destination_entry.get():
            messagebox.showwarning("Warning", "Please generate or input graph details.")
            return
            
        V = int(vertices_entry.get())
        E = int(edges_entry.get())
        src = int(source_entry.get())
        dest = int(destination_entry.get())
        
        if src >= V or dest >= V or src < 0 or dest < 0:
            messagebox.showerror("Error", "Source and Destination must be within 0 to V-1.")
            return

        edge_lines = text_input.get("1.0", tk.END).strip().split("\n")
        if not edge_lines or edge_lines[0] == "":
            messagebox.showwarning("Warning", "Please input edges.")
            return

        edges = []
        graph = [[] for _ in range(V)]
        
        has_negative_edge = False

        for line in edge_lines:
            parts = line.split()
            if len(parts) != 3: continue
            u, v, w = map(int, parts)
            if w < 0: has_negative_edge = True
            edge = Edge(u, v, w)
            edges.append(edge)
            graph[u].append(edge)

        # Improvement: Caching key includes src and dest, and chosen algorithm!
        selected_alg = algorithm_var.get() if (best_only or cache_mode) else "Compare"
        
        # Sort edges to ensure cache hits regardless of text input order or whitespace
        edges_tuple = tuple(sorted([(e.u, e.v, e.w) for e in edges]))
        graph_key = str((V, edges_tuple, src, dest, selected_alg))

        output_text.config(state="normal")
        output_text.delete("1.0", tk.END)
        output_text.config(state="disabled")

        # ================= CACHE =================
        if (best_only or cache_mode) and graph_key in learning_cache:
            current_start = time.perf_counter_ns()
            cached = learning_cache[graph_key]
            cost = cached["cost"]
            path = cached["path"]
            alg_name = cached["algorithm"]
            current_end = time.perf_counter_ns()

            current_time_ns = current_end - current_start
            current_time_ms = current_time_ns / 1_000_000
            old_time = cached["time_ms"]
            
            if cache_mode:
                if old_time > 0:
                    reduction = ((old_time - current_time_ms) / old_time) * 100
                else:
                    reduction = 0.0
                    
                write_output("===== CACHE ANALYSIS =====\n\n")
                write_output("Graph Present In Cache : YES\n")
                write_output(f"Stored Algorithm : {alg_name}\n")
                write_output(f"Previous Execution Time : {old_time:.5f} ms\n")
                write_output(f"Current Execution Time : {current_time_ms:.5f} ms\n")
                write_output(f"Time Reduced Percentage : {reduction:.2f}%\n")
                write_output(f"Shortest Path Cost : {cost if cost != INF else 'Unreachable'}\n")
                write_output(f"Shortest Path : {path if len(path) > 0 else 'None'}\n")
                
                return
            elif best_only:
                write_output("===== MANUAL TEST ANALYSIS (CACHED) =====\n\n")
                write_output(f"Selected Algorithm : {alg_name} (Cached)\n")
                write_output(f"Time Complexity    : O(1) Cache Lookup\n")
                write_output(f"Space Complexity   : O(V) Cached Path\n")
                write_output(f"Shortest Path Cost : {cost if cost != INF else 'Unreachable'}\n")
                write_output(f"Execution Time     : {current_time_ns} ns\n")
                write_output(f"Execution Time     : {current_time_ms:.5f} ms\n")
                write_output(f"Shortest Path      : {path if len(path) > 0 else 'None'}\n")
                
                return

        if cache_mode:
            write_output("===== CACHE ANALYSIS =====\n\nGraph Present In Cache : NO\n\nRunning Algorithm to cache the result...\n\n")

        # ================= OPTION 1 / MANUAL TEST =================
        if best_only or cache_mode:
            selected_alg = algorithm_var.get()
            
            if selected_alg == "Adaptive Best (Default)":
                if has_negative_edge:
                    alg_name = "Bellman-Ford (Adaptive: Negative edges present)"
                    dist, parent, elapsed, has_neg_cycle = bellman_ford(V, edges, src, dest)
                    time_comp = "O(V × E)"
                    space_comp = "O(V)"
                    if has_neg_cycle:
                        messagebox.showerror("Error", "Negative weight cycle detected!")
                        return
                else:
                    alg_name = "Dijkstra (Adaptive: Non-negative edges)"
                    dist, parent, elapsed = dijkstra(V, graph, src, dest)
                    time_comp = "O(E log V)"
                    space_comp = "O(V)"
                cost = dist[dest]
                path = get_path(parent, dest, cost)
                
            elif selected_alg == "Dijkstra":
                if has_negative_edge:
                    messagebox.showwarning("Warning", "Dijkstra may fail because negative edges are present!")
                alg_name = "Dijkstra"
                dist, parent, elapsed = dijkstra(V, graph, src, dest)
                cost = dist[dest]
                path = get_path(parent, dest, cost)
                time_comp = "O(E log V)"
                space_comp = "O(V)"
                
            elif selected_alg == "Bellman-Ford":
                alg_name = "Bellman-Ford"
                dist, parent, elapsed, has_neg_cycle = bellman_ford(V, edges, src, dest)
                if has_neg_cycle:
                    messagebox.showerror("Error", "Negative weight cycle detected!")
                    return
                cost = dist[dest]
                path = get_path(parent, dest, cost)
                time_comp = "O(V × E)"
                space_comp = "O(V)"
                
            elif selected_alg == "Floyd-Warshall":
                alg_name = "Floyd-Warshall"
                fw_dist, fw_nxt, elapsed, has_neg_cycle = floyd_warshall(V, edges, src, dest)
                if has_neg_cycle:
                    messagebox.showerror("Error", "Negative weight cycle detected!")
                    return
                cost = fw_dist[dest]
                path = get_fw_path(fw_nxt, src, dest, cost)
                time_comp = "O(V³)"
                space_comp = "O(V²)"
                
            elif selected_alg == "Johnson's":
                alg_name = "Johnson's"
                j_dist, j_parent, elapsed, has_neg_cycle = johnsons(V, edges, src, dest)
                if has_neg_cycle:
                    messagebox.showerror("Error", "Negative weight cycle detected!")
                    return
                cost = j_dist[dest]
                path = get_path(j_parent, dest, cost)
                time_comp = "O(V² log V + V E)"
                space_comp = "O(V²)"

            ms_time = elapsed / 1_000_000

            write_output("===== MANUAL TEST ANALYSIS =====\n\n")
            write_output(f"Selected Algorithm : {alg_name}\n")
            write_output(f"Time Complexity    : {time_comp}\n")
            write_output(f"Space Complexity   : {space_comp}\n")
            write_output(f"Shortest Path Cost : {cost if cost != INF else 'Unreachable'}\n")
            write_output(f"Execution Time     : {elapsed} ns\n")
            write_output(f"Execution Time     : {ms_time:.5f} ms\n")
            write_output(f"Shortest Path      : {path if len(path) > 0 else 'None'}\n")

            learning_cache[graph_key] = {
                "path": path,
                "time_ms": ms_time,
                "algorithm": alg_name.split(" ")[0],
                "cost": cost
            }
            save_cache(learning_cache)

        # ================= OPTION 2: Compare Algorithms =================
        else:
            d_dist, d_parent, d_time = dijkstra(V, graph, src, dest)
            b_dist, b_parent, b_time, has_neg_cycle = bellman_ford(V, edges, src, dest)
            fw_dist, fw_nxt, fw_time, fw_has_neg_cycle = floyd_warshall(V, edges, src, dest)
            j_dist, j_parent, j_time, j_has_neg_cycle = johnsons(V, edges, src, dest)

            if has_neg_cycle or fw_has_neg_cycle or j_has_neg_cycle:
                 messagebox.showwarning("Warning", "Negative cycle detected! Dijkstra's result is invalid, other algorithms caught the cycle.")
                 return

            d_ms = d_time / 1_000_000
            b_ms = b_time / 1_000_000
            fw_ms = fw_time / 1_000_000
            j_ms = j_time / 1_000_000

            write_output("===== COMPARISON TABLE =====\n\n")
            write_output("Algorithm\tTime(ms)\n")
            write_output(f"Dijkstra\t{d_ms:.5f}\n")
            write_output(f"BellmanFord\t{b_ms:.5f}\n")
            write_output(f"FloydWarshall\t{fw_ms:.5f}\n")
            write_output(f"Johnsons\t{j_ms:.5f}\n")
            
            # Identify the logically sound fastest algorithm
            if has_negative_edge:
                if b_ms <= j_ms:
                    fastest_algorithm = "Bellman-Ford (Dijkstra invalid due to neg edges)"
                    best_path = get_path(b_parent, dest, b_dist[dest])
                    best_cost = b_dist[dest]
                    best_time = b_ms
                    alg_cache_name = "Bellman-Ford"
                else:
                    fastest_algorithm = "Johnson's (Dijkstra invalid due to neg edges)"
                    best_path = get_path(j_parent, dest, j_dist[dest])
                    best_cost = j_dist[dest]
                    best_time = j_ms
                    alg_cache_name = "Johnson's"
            else:
                times_dict = {
                    "Dijkstra": (d_ms, get_path(d_parent, dest, d_dist[dest]), d_dist[dest]),
                    "Bellman-Ford": (b_ms, get_path(b_parent, dest, b_dist[dest]), b_dist[dest]),
                    "Floyd-Warshall": (fw_ms, get_fw_path(fw_nxt, src, dest, fw_dist[dest]), fw_dist[dest]),
                    "Johnson's": (j_ms, get_path(j_parent, dest, j_dist[dest]), j_dist[dest])
                }
                fastest_algorithm = min(times_dict, key=lambda k: times_dict[k][0])
                best_time, best_path, best_cost = times_dict[fastest_algorithm]
                alg_cache_name = fastest_algorithm

            write_output(f"\nRecommended Algorithm : {fastest_algorithm}\n")
            write_output(f"Shortest Path Cost : {best_cost if best_cost != INF else 'Unreachable'}\n")

            comparison_chart([d_ms, b_ms, fw_ms, j_ms])

            learning_cache[graph_key] = {
                "path": best_path,
                "time_ms": best_time,
                "algorithm": alg_cache_name,
                "cost": best_cost
            }
            save_cache(learning_cache)

    except Exception as e:
        import traceback
        traceback.print_exc()
        messagebox.showerror("Error", str(e))


# ================= BUTTON FUNCTIONS =================
def best_algorithm():
    run_algorithms(best_only=True)

def compare_algorithms():
    run_algorithms(best_only=False)

def cache_prediction():
    run_algorithms(cache_mode=True)

def show_graph_manually():
    try:
        if not vertices_entry.get() or not edges_entry.get() or not source_entry.get() or not destination_entry.get():
            messagebox.showwarning("Warning", "Please generate or input graph details.")
            return
            
        V = int(vertices_entry.get())
        src = int(source_entry.get())
        dest = int(destination_entry.get())

        edge_lines = text_input.get("1.0", tk.END).strip().split("\n")
        if not edge_lines or edge_lines[0] == "":
            messagebox.showwarning("Warning", "Please input edges.")
            return

        edges = []
        for line in edge_lines:
            parts = line.split()
            if len(parts) != 3: continue
            u, v, w = map(int, parts)
            edges.append(Edge(u, v, w))

        # Check if we have a path cached for this exact configuration
        path = None
        selected_alg = algorithm_var.get()
        
        edges_tuple = tuple(sorted([(e.u, e.v, e.w) for e in edges]))
        graph_key_opt1 = str((V, edges_tuple, src, dest, selected_alg))
        graph_key_opt2 = str((V, edges_tuple, src, dest, "Compare"))
        
        if graph_key_opt1 in learning_cache:
            path = learning_cache[graph_key_opt1]["path"]
        elif graph_key_opt2 in learning_cache:
            path = learning_cache[graph_key_opt2]["path"]
            
        draw_graphs(V, edges, path, graph_mode.get())
    except Exception as e:
        messagebox.showerror("Error", f"Could not draw graph: {e}")


# ================= GUI =================
if __name__ == "__main__":
    root = tk.Tk()
    root.title("AHSPA-LC using Dijkstra and Bellman-Ford")
    root.geometry("1100x950")
    root.configure(bg="#EAF2F8")

    # ================= TITLE =================
    title = tk.Label(
        root,
        text="Adaptive Hybrid Shortest Path\nAlgorithm with Learning Cache (AHSPA-LC)",
        font=("Arial", 18, "bold"),
        bg="#EAF2F8",
        fg="darkblue"
    )
    title.pack(pady=10)

    # ================= INPUT FRAME =================
    frame = tk.Frame(root, bg="#EAF2F8")
    frame.pack()

    labels = ["Vertices", "Edges", "Source", "Destination"]
    entries = []

    for i, text in enumerate(labels):
        tk.Label(frame, text=text, font=("Arial", 12, "bold"), bg="#EAF2F8").grid(row=i, column=0, padx=10, pady=5)
        entry = tk.Entry(frame, width=15)
        entry.grid(row=i, column=1)
        entries.append(entry)

    vertices_entry, edges_entry, source_entry, destination_entry = entries

    # ================= TEXT INPUT =================
    tk.Label(root, text="Enter Graph Edges: u v w", font=("Arial", 12), bg="#EAF2F8").pack()
    text_input = tk.Text(root, height=12, width=80)
    text_input.pack(pady=10)

    # ================= BUTTONS =================
    button_frame = tk.Frame(root, bg="#EAF2F8")
    button_frame.pack(pady=10)

    tk.Button(button_frame, text="Option 1\nRun Algorithm", width=20, height=3, bg="darkblue", fg="white", font=("Arial", 10, "bold"), command=best_algorithm).grid(row=0, column=0, padx=10)
    tk.Button(button_frame, text="Option 2\nCompare Algorithms", width=20, height=3, bg="green", fg="white", font=("Arial", 10, "bold"), command=compare_algorithms).grid(row=0, column=1, padx=10)
    tk.Button(button_frame, text="Option 3\nLearning Cache", width=20, height=3, bg="purple", fg="white", font=("Arial", 10, "bold"), command=cache_prediction).grid(row=0, column=2, padx=10)

    tk.Button(root, text="Generate Random Graph", bg="orange", fg="black", font=("Arial", 12, "bold"), command=generate_graph).pack(pady=10)

    # ================= ALGORITHM SELECTOR =================
    algo_frame = tk.Frame(root, bg="#EAF2F8")
    algo_frame.pack(pady=5)
    tk.Label(algo_frame, text="Select Algorithm for Option 1:", font=("Arial", 12, "bold"), bg="#EAF2F8").pack(side=tk.LEFT, padx=10)
    algorithm_var = tk.StringVar()
    algorithm_dropdown = ttk.Combobox(algo_frame, textvariable=algorithm_var, state="readonly", font=("Arial", 12), width=25)
    algorithm_dropdown['values'] = ("Adaptive Best (Default)", "Dijkstra", "Bellman-Ford", "Floyd-Warshall", "Johnson's")
    algorithm_dropdown.current(0)
    algorithm_dropdown.pack(side=tk.LEFT)

    # ================= 3D OPTION & SHOW GRAPH =================
    graph_frame = tk.Frame(root, bg="#EAF2F8")
    graph_frame.pack(pady=5)
    
    graph_mode = tk.BooleanVar()
    graph_mode.set(False)
    tk.Checkbutton(graph_frame, text="View Graph in 3D", variable=graph_mode, font=("Arial", 12, "bold"), bg="#EAF2F8").grid(row=0, column=0, padx=10)
    
    tk.Button(graph_frame, text="Show Graphical Representation", bg="cyan", fg="black", font=("Arial", 10, "bold"), command=show_graph_manually).grid(row=0, column=1, padx=10)

    # ================= OUTPUT =================
    output_text = tk.Text(root, height=20, width=120, font=("Consolas", 10), state="disabled", bg="#FDFEFE")
    output_text.pack(pady=10)

    # ================= MAIN LOOP =================
    root.mainloop()
